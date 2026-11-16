package metro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import metro.exception.ScheduleConflictException;
import metro.exception.ValidationException;
import metro.model.Direction;
import metro.model.Line;
import metro.model.Permission;
import metro.model.Train;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.repository.AuditLog;
import metro.repository.TripRepository;
import metro.util.Dates;

/**
 * The rules of the schedule (specification module 2): creating trips,
 * changing and cancelling them, tracking their status, and refusing a
 * schedule that would put one train on two trips at the same time.
 */
public class TripService {

    /** Minutes a train needs between the end of one trip and the start of the next. */
    public static final int TURNAROUND_GAP_MINUTES = 5;
    /** Travel time used to suggest an arrival: this many minutes per stop. */
    public static final int MINUTES_PER_STOP = 3;
    public static final int MAX_DELAY_MINUTES = 240;

    private final TripRepository trips;
    private final Session session;
    private final AuditLog audit;
    private final Clock clock;

    public TripService(TripRepository trips, Session session, AuditLog audit, Clock clock) {
        this.trips = trips;
        this.session = session;
        this.audit = audit;
        this.clock = clock;
    }

    /** Puts trips in order of departure time. */
    private static class ByDeparture implements Comparator<Trip> {
        @Override
        public int compare(Trip a, Trip b) {
            return a.getDeparture().compareTo(b.getDeparture());
        }
    }

    public List<Trip> listAll() {
        session.require(Permission.VIEW_TRIPS);
        List<Trip> all = trips.findAll();
        all.sort(new ByDeparture());
        return all;
    }

    /**
     * Trips matching every filter that is given. A null (or blank) argument
     * means "do not filter on this". Dates compare the day of departure and
     * include both ends. The text matches the trip id, the train ID, the
     * line name and the names of the first and last station.
     */
    public List<Trip> find(String text, LocalDate from, LocalDate to, Line line, TripStatus status) {
        session.require(Permission.VIEW_TRIPS);
        String wanted = text == null ? "" : text.trim().toLowerCase();
        List<Trip> found = new ArrayList<>();
        for (Trip trip : trips.findAll()) {
            LocalDate day = trip.getDeparture().toLocalDate();
            if (from != null && day.isBefore(from)) {
                continue;
            }
            if (to != null && day.isAfter(to)) {
                continue;
            }
            if (line != null && !trip.getLine().equals(line)) {
                continue;
            }
            if (status != null && trip.getStatus() != status) {
                continue;
            }
            if (!wanted.isEmpty() && !matches(trip, wanted)) {
                continue;
            }
            found.add(trip);
        }
        found.sort(new ByDeparture());
        return found;
    }

    private boolean matches(Trip trip, String wanted) {
        return trip.getId().toLowerCase().contains(wanted)
                || trip.getTrain().getCode().toLowerCase().contains(wanted)
                || trip.getLine().getName().toLowerCase().contains(wanted)
                || trip.getOrigin().getName().toLowerCase().contains(wanted)
                || trip.getDestination().getName().toLowerCase().contains(wanted);
    }

    /** A sensible arrival time for a trip along the whole line. */
    public LocalDateTime suggestArrival(Line line, LocalDateTime departure) {
        session.require(Permission.VIEW_TRIPS);
        return departure.plusMinutes((long) (line.size() - 1) * MINUTES_PER_STOP);
    }

    /**
     * The conflict detector (specification 2d).
     *
     * Returns the trips of the same train whose busy time overlaps the given
     * time range, earliest first. Cancelled trips never count, and the trip
     * being edited (ignoreTripId) does not clash with itself.
     */
    public List<Trip> findConflicts(Train train, LocalDateTime departure,
                                    LocalDateTime arrival, String ignoreTripId) {
        session.require(Permission.VIEW_TRIPS);
        List<Trip> conflicts = new ArrayList<>();
        for (Trip other : trips.findByTrain(train)) {
            if (other.getId().equals(ignoreTripId) || other.getStatus() == TripStatus.CANCELLED) {
                continue;
            }
            if (other.overlaps(departure, arrival, TURNAROUND_GAP_MINUTES)) {
                conflicts.add(other);
            }
        }
        conflicts.sort(new ByDeparture());
        return conflicts;
    }

    /** Schedules a new trip (specification 2a). */
    public Trip create(Train train, Line line, Direction direction,
                       LocalDateTime departure, LocalDateTime arrival) {
        session.require(Permission.MANAGE_TRIPS);
        check(train, line, direction, departure, arrival, null);
        Trip trip = new Trip(trips.nextId(), train, line, direction, departure, arrival);
        trips.save(trip);
        record("TRIP_CREATED", trip);
        return trip;
    }

    /** Changes a trip that has not finished yet (specification 2b). */
    public Trip update(String id, Train train, Line line, Direction direction,
                       LocalDateTime departure, LocalDateTime arrival) {
        session.require(Permission.MANAGE_TRIPS);
        Trip trip = trips.require(id);
        if (trip.getStatus().isFinal()) {
            throw new ValidationException("A " + trip.getStatus().getDisplayName() + " trip cannot be edited");
        }
        check(train, line, direction, departure, arrival, id);
        trip.reschedule(train, line, direction, departure, arrival);
        trips.save(trip);
        record("TRIP_UPDATED", trip);
        return trip;
    }

    /**
     * Everything that must hold before a trip is created or changed. Nothing
     * has been stored yet when one of these checks refuses.
     *
     * Rule: only an active train can be scheduled.
     * Rule: arrival must be after departure (checked by building a trial trip).
     * Rule: the train must not already be busy in that time range.
     */
    private void check(Train train, Line line, Direction direction,
                       LocalDateTime departure, LocalDateTime arrival, String ignoreTripId) {
        if (train != null && !train.isActive()) {
            throw new ValidationException("Train " + train.getCode() + " is "
                    + train.getStatus().getDisplayName() + " and cannot be scheduled");
        }
        new Trip("TRIAL", train, line, direction, departure, arrival);
        List<Trip> conflicts = findConflicts(train, departure, arrival, ignoreTripId);
        if (!conflicts.isEmpty()) {
            throw new ScheduleConflictException(train, conflicts);
        }
    }

    public Trip cancel(String id) {
        session.require(Permission.MANAGE_TRIPS);
        Trip trip = trips.require(id);
        trip.changeStatus(TripStatus.CANCELLED);
        trips.save(trip);
        record("TRIP_CANCELLED", trip);
        return trip;
    }

    public Trip complete(String id) {
        session.require(Permission.MANAGE_TRIPS);
        Trip trip = trips.require(id);
        trip.changeStatus(TripStatus.COMPLETED);
        trips.save(trip);
        record("TRIP_COMPLETED", trip);
        return trip;
    }

    /**
     * Marks a trip as delayed by a number of minutes (replacing any earlier delay).
     *
     * A delay is a fact, so it is always saved. If the later arrival now
     * clashes with the train's next trip, those trips are returned so the
     * caller can warn the user to reschedule or cancel them.
     */
    public List<Trip> delay(String id, int minutes) {
        session.require(Permission.MANAGE_TRIPS);
        Trip trip = trips.require(id);
        if (minutes < 1 || minutes > MAX_DELAY_MINUTES) {
            throw new ValidationException("Delay must be between 1 and " + MAX_DELAY_MINUTES + " minutes");
        }
        trip.changeStatus(TripStatus.DELAYED);
        trip.setDelayMinutes(minutes);
        trips.save(trip);
        record("TRIP_DELAYED", trip);
        return findConflicts(trip.getTrain(), trip.getDeparture(), trip.getEffectiveArrival(), trip.getId());
    }

    /** How many trips of a train are still to be run (Scheduled or Delayed). */
    public int countUpcoming(Train train) {
        session.require(Permission.VIEW_TRIPS);
        return trips.countUnfinished(train);
    }

    /** The current date and time. */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void record(String action, Trip trip) {
        audit.record(session.getUser().getUsername(), action,
                trip.getId() + " " + trip.getLine().getName() + " " + Dates.time(trip.getDeparture()));
    }
}
