package metro.model;

import java.time.LocalDateTime;
import metro.exception.ValidationException;

/**
 * One scheduled run of a train along a line, with a departure and an
 * arrival time and a service status.
 *
 * A trip refers to the Train that runs it and the Line it runs on
 * (association): it uses them but does not own them.
 */
public class Trip extends Entity {

    private Train train;
    private Line line;
    private Direction direction;
    private LocalDateTime departure;
    private LocalDateTime arrival;
    private TripStatus status;
    private int delayMinutes;

    /** A new trip: scheduled, with no delay. */
    public Trip(String id, Train train, Line line, Direction direction,
                LocalDateTime departure, LocalDateTime arrival) {
        this(id, train, line, direction, departure, arrival, TripStatus.SCHEDULED, 0);
    }

    /** A trip with a known status and delay; used when trips are read from the file. */
    public Trip(String id, Train train, Line line, Direction direction,
                LocalDateTime departure, LocalDateTime arrival, TripStatus status, int delayMinutes) {
        super(id);
        reschedule(train, line, direction, departure, arrival);
        if (status == null) {
            throw new IllegalArgumentException("A trip needs a status");
        }
        this.status = status;
        setDelayMinutes(delayMinutes);
    }

    public Train getTrain() {
        return train;
    }

    public Line getLine() {
        return line;
    }

    public Direction getDirection() {
        return direction;
    }

    public LocalDateTime getDeparture() {
        return departure;
    }

    /** The planned arrival, without any delay. */
    public LocalDateTime getArrival() {
        return arrival;
    }

    public TripStatus getStatus() {
        return status;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    /**
     * Sets everything that defines when and where the trip runs.
     * Rule: the arrival must be after the departure. Nothing is changed when it is not.
     */
    public void reschedule(Train train, Line line, Direction direction,
                           LocalDateTime departure, LocalDateTime arrival) {
        if (train == null || line == null || direction == null || departure == null || arrival == null) {
            throw new ValidationException("A trip needs a train, a line, a direction and two times");
        }
        if (!arrival.isAfter(departure)) {
            throw new ValidationException("Arrival must be after departure");
        }
        this.train = train;
        this.line = line;
        this.direction = direction;
        this.departure = departure;
        this.arrival = arrival;
    }

    /** Rule: only the status changes listed in TripStatus are allowed. */
    public void changeStatus(TripStatus next) {
        if (!status.canChangeTo(next)) {
            throw new ValidationException(
                    "A " + status.getDisplayName() + " trip cannot become " + next.getDisplayName());
        }
        this.status = next;
    }

    public void setDelayMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("A delay cannot be negative");
        }
        this.delayMinutes = minutes;
    }

    /** When the train is really expected to arrive: the planned arrival plus the delay. */
    public LocalDateTime getEffectiveArrival() {
        return arrival.plusMinutes(delayMinutes);
    }

    /** The station the trip starts from, which depends on its direction. */
    public Station getOrigin() {
        return direction == Direction.FORWARD ? line.getFirstStation() : line.getLastStation();
    }

    public Station getDestination() {
        return direction == Direction.FORWARD ? line.getLastStation() : line.getFirstStation();
    }

    /**
     * True when this trip's busy time overlaps another busy time, counting a
     * gap of some minutes that a train needs between two trips.
     *
     * Two time ranges overlap when each one starts before the other one ends.
     * This method compares times only; the caller decides which trips to compare.
     */
    public boolean overlaps(LocalDateTime otherDeparture, LocalDateTime otherEffectiveArrival, int gapMinutes) {
        return departure.isBefore(otherEffectiveArrival.plusMinutes(gapMinutes))
                && otherDeparture.isBefore(getEffectiveArrival().plusMinutes(gapMinutes));
    }
}
