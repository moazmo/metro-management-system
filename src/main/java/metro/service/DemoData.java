package metro.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import metro.exception.DataFileException;
import metro.model.Line;
import metro.model.Role;
import metro.model.Station;
import metro.model.StationStatus;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.model.UserFactory;
import metro.repository.LineRepository;
import metro.repository.StationRepository;
import metro.repository.TrainRepository;
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
