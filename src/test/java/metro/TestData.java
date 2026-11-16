package metro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import metro.model.Role;
import metro.model.User;
import metro.model.UserFactory;
import metro.repository.AuditLog;
import metro.repository.UserRepository;
import metro.service.AuthService;
import metro.service.PasswordHasher;
import metro.service.Session;
import metro.service.UserService;

/**
 * Shared set-up for the tests: repositories and services on a temporary
 * folder, a clock fixed at 2026-11-15 10:00, and one user per role.
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
    public final AuthService auth;
    public final UserService userService;

    public final User admin;
    public final User controller;
    public final User agent;
    public final User supervisor;

    public TestData(Path dir) {
        this.dir = dir;
        audit = new AuditLog(dir.resolve("audit.log"), clock);
        users = new UserRepository(dir.resolve("users.txt"));
        users.load();
        admin = addUser(Role.ADMIN, "admin", "System Admin");
        controller = addUser(Role.OPERATIONS_CONTROLLER, "controller", "Operations Desk");
        agent = addUser(Role.TICKET_AGENT, "agent", "Ticket Window 1");
        supervisor = addUser(Role.STATION_SUPERVISOR, "supervisor", "Station Supervisor");
        auth = new AuthService(users, session, audit);
        userService = new UserService(users, session, audit);
    }

    private User addUser(Role role, String username, String fullName) {
        User user = UserFactory.create(role, users.nextId(), username, HASH, SALT, fullName, "", "", true);
        users.save(user);
        return user;
    }

    public void loginAs(User user) {
        session.login(user);
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
