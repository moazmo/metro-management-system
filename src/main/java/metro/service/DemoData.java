package metro.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import metro.exception.DataFileException;
import metro.model.Direction;
import metro.model.FareTier;
import metro.model.Line;
import metro.model.Role;
import metro.model.Station;
import metro.model.StationStatus;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.model.UserFactory;
import metro.repository.FareTierRepository;
import metro.repository.LineRepository;
import metro.repository.StationRepository;
import metro.repository.TrainRepository;
import metro.repository.TripRepository;
import metro.repository.UserRepository;
import metro.util.Fields;

/**
 * Creates the data the application starts with on its first run, so that
 * there is something to log in to and something to show.
 */
public final class DemoData {

    /** The password of every demo account. */
    public static final String DEMO_PASSWORD = "metro123";

    private static final String NETWORK_FILE = "/metro/seed/network.txt";

    private DemoData() {
    }

    /** One account per role. Each gets its own salt, so the stored hashes differ. */
    public static void seedUsers(UserRepository users) {
        addUser(users, Role.ADMIN, "admin", "System Admin");
        addUser(users, Role.OPERATIONS_CONTROLLER, "controller", "Operations Desk");
        addUser(users, Role.TICKET_AGENT, "agent", "Ticket Window 1");
        addUser(users, Role.STATION_SUPERVISOR, "supervisor", "Station Supervisor");
    }

    private static void addUser(UserRepository users, Role role, String username, String fullName) {
        String salt = PasswordHasher.newSalt();
        users.save(UserFactory.create(role, users.nextId(), username,
                PasswordHasher.hash(DEMO_PASSWORD, salt), salt, fullName, "", "", true));
    }

    /**
     * The demo network (stations and lines read from network.txt) and a
     * fleet of twelve trains.
     *
     * A station is created the first time its name appears. When the same
     * name appears on another line, the existing station is reused, which is
     * what makes it an interchange.
     */
    public static void seedNetwork(StationRepository stations, LineRepository lines, TrainRepository trains) {
        for (String text : readResourceLines(NETWORK_FILE)) {
            if (text.isBlank() || text.startsWith("#")) {
                continue;
            }
            String[] fields = Fields.split(text); // name | colour | stations
            List<Station> onLine = new ArrayList<>();
            for (String pair : fields[2].split(";")) {
                int equals = pair.indexOf('=');
                String english = pair.substring(0, equals).trim();
                String arabic = pair.substring(equals + 1).trim();
                Station station = stations.findByName(english);
                if (station == null) {
                    station = new Station(stations.nextId(), english, arabic, StationStatus.OPEN);
                    stations.save(station);
                }
                onLine.add(station);
            }
            lines.save(new Line(lines.nextId(), fields[0], fields[1], onLine));
        }

        for (int number = 101; number <= 112; number++) {
            TrainStatus status = number == 112 ? TrainStatus.MAINTENANCE : TrainStatus.ACTIVE;
            trains.save(new Train(trains.nextId(), "T-" + number, 1800, status));
        }
    }

    /**
     * The fare table: up to 9 stops, up to 16, up to 23 and up to 39.
     * These are example prices; an admin can change them on the Fares screen.
     */
    public static void seedFares(FareTierRepository tiers) {
        List<FareTier> table = new ArrayList<>();
        table.add(new FareTier(tiers.nextId(), 9, 8.00));
        table.add(new FareTier(tiers.nextId(), 16, 10.00));
        table.add(new FareTier(tiers.nextId(), 23, 15.00));
        table.add(new FareTier(tiers.nextId(), 39, 20.00));
        tiers.saveAll(table);
    }

    /**
     * A week of trips around today: three days back, today, three days ahead.
     * Every line gets six departures a day in each direction.
     *
     * Trains are handed out so that no train is on two trips at once: for each
     * train we remember when it becomes free, and each trip takes the first
     * train that is free at its departure time. Past trips are completed (one
     * a day is cancelled), today has two delayed trips, later trips are scheduled.
     * Nothing here is random, so the same day always gives the same timetable.
     */
    public static void seedTrips(TripRepository trips, List<Line> lines, List<Train> allTrains,
                                 LocalDateTime now) {
        List<Train> trains = new ArrayList<>();
        for (Train train : allTrains) {
            if (train.isActive()) {
                trains.add(train);
            }
        }
        LocalDateTime[] freeAt = new LocalDateTime[trains.size()];
        LocalDate today = now.toLocalDate();
        int[] hours = {6, 9, 12, 15, 18, 21};
        List<Trip> all = new ArrayList<>();
        List<Trip> todays = new ArrayList<>();

        for (int offset = -3; offset <= 3; offset++) {
            LocalDate day = today.plusDays(offset);
            int tripOfDay = 0;
            for (int hour : hours) {
                for (Direction direction : Direction.values()) {
                    for (Line line : lines) {
                        // forward trips leave on the hour, backward trips half an hour later
                        LocalDateTime departure = day.atTime(hour, direction == Direction.FORWARD ? 0 : 30);
                        LocalDateTime arrival = departure.plusMinutes(
                                (long) (line.size() - 1) * TripService.MINUTES_PER_STOP);
                        int chosen = firstFreeTrain(freeAt, departure);
                        // keep room for the turnaround gap and for a demo delay of up to 15 minutes
                        freeAt[chosen] = arrival.plusMinutes(TripService.TURNAROUND_GAP_MINUTES + 15);

                        Trip trip = new Trip(trips.nextId(), trains.get(chosen), line, direction, departure, arrival);
                        if (offset < 0) {
                            trip.changeStatus(tripOfDay == 2 ? TripStatus.CANCELLED : TripStatus.COMPLETED);
                        } else if (offset == 0) {
                            todays.add(trip);
                        }
                        all.add(trip);
                        tripOfDay++;
                    }
                }
            }
        }

        // Today: the next two departures are delayed; trips that have already arrived are completed.
        List<Trip> delayed = new ArrayList<>();
        for (Trip trip : todays) {
            if (delayed.size() < 2 && trip.getDeparture().isAfter(now)) {
                delayed.add(trip);
            }
        }
        for (int i = todays.size() - 1; i >= 0 && delayed.size() < 2; i--) {
            if (!delayed.contains(todays.get(i))) {
                delayed.add(todays.get(i));
            }
        }
        for (Trip trip : todays) {
            int position = delayed.indexOf(trip);
            if (position >= 0) {
                trip.changeStatus(TripStatus.DELAYED);
                trip.setDelayMinutes(position == 0 ? 10 : 15);
            } else if (trip.getArrival().isBefore(now)) {
                trip.changeStatus(TripStatus.COMPLETED);
            }
        }
        trips.saveAll(all);
    }

    /** The position of the first train that is free at the given time. */
    private static int firstFreeTrain(LocalDateTime[] freeAt, LocalDateTime departure) {
        for (int i = 0; i < freeAt.length; i++) {
            if (freeAt[i] == null || !freeAt[i].isAfter(departure)) {
                return i;
            }
        }
        throw new IllegalStateException("The demo fleet is too small for the demo timetable");
    }

    /** Reads a text file that is packaged inside the application. */
    private static List<String> readResourceLines(String resource) {
        List<String> lines = new ArrayList<>();
        try (InputStream in = DemoData.class.getResourceAsStream(resource);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException | NullPointerException e) {
            throw new DataFileException("Could not read the demo data " + resource, e);
        }
        return lines;
    }
}
