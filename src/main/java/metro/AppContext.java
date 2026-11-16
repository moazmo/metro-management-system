package metro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import metro.exception.DataFileException;
import metro.repository.AuditLog;
import metro.repository.LineRepository;
import metro.repository.StationRepository;
import metro.repository.TrainRepository;
import metro.repository.TripRepository;
import metro.repository.UserRepository;
import metro.service.AuthService;
import metro.service.DemoData;
import metro.service.LineService;
import metro.service.RoutePlanner;
import metro.service.Session;
import metro.service.StationService;
import metro.service.TrainService;
import metro.service.TripService;
import metro.service.UserService;

/**
 * Builds every repository and service once, when the application starts,
 * and hands them to the screens.
 *
 * Creating everything in one place keeps the order clear: the data folder
 * first, then the repositories (loaded so that a record's references already
 * exist), then the services that use them. On the very first run the demo
 * data is created.
 */
public class AppContext {

    private final Path dataDir;
    private final Clock clock;
    private final Session session = new Session();
    private final AuditLog auditLog;

    private final UserRepository users;
    private final StationRepository stations;
    private final LineRepository lines;
    private final TrainRepository trains;
    private final TripRepository trips;

    private final AuthService authService;
    private final UserService userService;
    private final StationService stationService;
    private final LineService lineService;
    private final TrainService trainService;
    private final TripService tripService;
    private final RoutePlanner routePlanner;

    public AppContext(Path dataDir, Clock clock) {
        this.dataDir = dataDir;
        this.clock = clock;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new DataFileException("Could not create the data folder " + dataDir, e);
        }
        boolean firstRun = !Files.exists(dataDir.resolve("users.txt"));

        // Repositories, loaded in an order where everything a record refers to is already there.
        auditLog = new AuditLog(dataDir.resolve("audit.log"), clock);
        users = new UserRepository(dataDir.resolve("users.txt"));
        users.load();
        stations = new StationRepository(dataDir.resolve("stations.txt"));
        stations.load();
        lines = new LineRepository(dataDir.resolve("lines.txt"), stations);
        lines.load();
        trains = new TrainRepository(dataDir.resolve("trains.txt"));
        trains.load();
        trips = new TripRepository(dataDir.resolve("trips.txt"), trains, lines);
        trips.load();

        if (firstRun) {
            DemoData.seedUsers(users);
            DemoData.seedNetwork(stations, lines, trains);
            DemoData.seedTrips(trips, lines.findAll(), trains.findAll(), LocalDateTime.now(clock));
        }

        // Services.
        authService = new AuthService(users, session, auditLog);
        userService = new UserService(users, session, auditLog);
        stationService = new StationService(stations, lines, session, auditLog);
        lineService = new LineService(lines, trips, session, auditLog);
        trainService = new TrainService(trains, trips, session, auditLog);
        tripService = new TripService(trips, session, auditLog, clock);
        routePlanner = new RoutePlanner(lines);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Clock getClock() {
        return clock;
    }

    public Session getSession() {
        return session;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public StationService getStationService() {
        return stationService;
    }

    public LineService getLineService() {
        return lineService;
    }

    public TrainService getTrainService() {
        return trainService;
    }

    public TripService getTripService() {
        return tripService;
    }

    public RoutePlanner getRoutePlanner() {
        return routePlanner;
    }
}
