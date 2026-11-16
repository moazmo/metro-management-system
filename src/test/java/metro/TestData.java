package metro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import metro.model.Line;
import metro.model.Role;
import metro.model.Station;
import metro.model.StationStatus;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.model.User;
import metro.model.UserFactory;
import metro.repository.AuditLog;
import metro.repository.FareTierRepository;
import metro.repository.LineRepository;
import metro.repository.StationRepository;
import metro.repository.TicketRepository;
import metro.repository.TrainRepository;
import metro.repository.TripRepository;
import metro.repository.UserRepository;
import metro.service.AuthService;
import metro.service.DemoData;
import metro.service.FareService;
import metro.service.LineService;
import metro.service.PasswordHasher;
import metro.service.Session;
import metro.service.StationService;
import metro.service.TrainService;
import metro.service.TripService;
import metro.service.UserService;

/**
 * Shared set-up for the tests: repositories and services on a temporary
 * folder, a clock fixed at 2026-11-15 10:00, one user per role, and a small
 * fixture network:
 *
 *   Red    A B C D E
 *   Blue   F C G H        (meets Red at C)
 *   Green  H E I          (meets Blue at H and Red at E)
 *   Island X Y            (not connected to the others)
 *
 * Stations get the ids STN-001 (A) to STN-011 (Y); users USR-001 (admin) to
 * USR-004 (supervisor); trains TRN-001 to TRN-003.
 */
public class TestData {

    public static final String PASSWORD = "metro123";
    // Hashing is slow on purpose, so the test password is hashed once and reused.
    private static final String SALT = PasswordHasher.newSalt();
    private static final String HASH = PasswordHasher.hash(PASSWORD, SALT);

    public final Path dir;
    public final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 11, 15, 10, 0));
    public final Session session = new Session();
    public final AuditLog audit;

    public final UserRepository users;
    public final StationRepository stations;
    public final LineRepository lines;
    public final TrainRepository trains;
    public final TripRepository trips;
    public final FareTierRepository fareTiers;
    public final TicketRepository tickets;

    public final AuthService auth;
    public final UserService userService;
    public final StationService stationService;
    public final LineService lineService;
    public final TrainService trainService;
    public final TripService tripService;
    public final FareService fareService;

    public final User admin, controller, agent, supervisor;
    public final Station stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH, stationI;
    public final Station stationX, stationY;
    public final Line red, blue, green, island;
    public final Train train1, train2, train3;

    public TestData(Path dir) {
        this.dir = dir;
        audit = new AuditLog(dir.resolve("audit.log"), clock);

        users = new UserRepository(dir.resolve("users.txt"));
        users.load();
        stations = new StationRepository(dir.resolve("stations.txt"));
        stations.load();
        lines = new LineRepository(dir.resolve("lines.txt"), stations);
        lines.load();
        trains = new TrainRepository(dir.resolve("trains.txt"));
        trains.load();
        trips = new TripRepository(dir.resolve("trips.txt"), trains, lines);
        trips.load();
        fareTiers = new FareTierRepository(dir.resolve("fares.txt"));
        fareTiers.load();
        DemoData.seedFares(fareTiers); // up to 9 stops 8.00, 16 -> 10.00, 23 -> 15.00, 39 -> 20.00
        tickets = new TicketRepository(dir.resolve("tickets.txt"), stations);
        tickets.load();

        admin = addUser(Role.ADMIN, "admin", "System Admin");
        controller = addUser(Role.OPERATIONS_CONTROLLER, "controller", "Operations Desk");
        agent = addUser(Role.TICKET_AGENT, "agent", "Ticket Window 1");
        supervisor = addUser(Role.STATION_SUPERVISOR, "supervisor", "Station Supervisor");

        stationA = addStation("A");
        stationB = addStation("B");
        stationC = addStation("C");
        stationD = addStation("D");
        stationE = addStation("E");
        stationF = addStation("F");
        stationG = addStation("G");
        stationH = addStation("H");
        stationI = addStation("I");
        stationX = addStation("X");
        stationY = addStation("Y");
        red = addLine("Red", "#D32F2F", stationA, stationB, stationC, stationD, stationE);
        blue = addLine("Blue", "#1976D2", stationF, stationC, stationG, stationH);
        green = addLine("Green", "#388E3C", stationH, stationE, stationI);
        island = addLine("Island", "#757575", stationX, stationY);

        train1 = addTrain("T-101", 1200, TrainStatus.ACTIVE);
        train2 = addTrain("T-102", 1200, TrainStatus.ACTIVE);
        train3 = addTrain("T-103", 1000, TrainStatus.MAINTENANCE);

        auth = new AuthService(users, session, audit);
        userService = new UserService(users, session, audit);
        stationService = new StationService(stations, lines, session, audit);
        lineService = new LineService(lines, trips, session, audit);
        trainService = new TrainService(trains, trips, session, audit);
        tripService = new TripService(trips, session, audit, clock);
        fareService = new FareService(fareTiers, session, audit);
    }

    private User addUser(Role role, String username, String fullName) {
        User user = UserFactory.create(role, users.nextId(), username, HASH, SALT, fullName, "", "", true);
        users.save(user);
        return user;
    }

    private Station addStation(String name) {
        Station station = new Station(stations.nextId(), name, "", StationStatus.OPEN);
        stations.save(station);
        return station;
    }

    private Line addLine(String name, String colour, Station... onLine) {
        Line line = new Line(lines.nextId(), name, colour, Arrays.asList(onLine));
        lines.save(line);
        return line;
    }

    private Train addTrain(String code, int capacity, TrainStatus status) {
        Train train = new Train(trains.nextId(), code, capacity, status);
        trains.save(train);
        return train;
    }

    public void loginAs(User user) {
        session.login(user);
    }

    /** A time on the fixture's "today", 2026-11-15. */
    public static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 15, hour, minute);
    }

    /** A time on a given day of November 2026. */
    public static LocalDateTime on(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute);
    }

    /** Lines read again from the files by brand-new repositories, as after a restart. */
    public LineRepository reloadedLines() {
        StationRepository freshStations = new StationRepository(dir.resolve("stations.txt"));
        freshStations.load();
        LineRepository freshLines = new LineRepository(dir.resolve("lines.txt"), freshStations);
        freshLines.load();
        return freshLines;
    }

    /** The lines of one data file in the temporary folder. */
    public List<String> lines(String fileName) {
        try {
            return Files.readAllLines(dir.resolve(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The first line of a data file that starts with the given text, or null. */
    public String lineStarting(String fileName, String start) {
        for (String line : lines(fileName)) {
            if (line.startsWith(start)) {
                return line;
            }
        }
        return null;
    }
}
