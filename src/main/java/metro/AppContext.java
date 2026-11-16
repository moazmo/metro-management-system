package metro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import metro.exception.DataFileException;
import metro.repository.AuditLog;
import metro.repository.UserRepository;
import metro.service.AuthService;
import metro.service.DemoData;
import metro.service.Session;
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
    private final AuthService authService;
    private final UserService userService;

    public AppContext(Path dataDir, Clock clock) {
        this.dataDir = dataDir;
        this.clock = clock;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new DataFileException("Could not create the data folder " + dataDir, e);
        }
        boolean firstRun = !Files.exists(dataDir.resolve("users.txt"));

        auditLog = new AuditLog(dataDir.resolve("audit.log"), clock);
        users = new UserRepository(dataDir.resolve("users.txt"));
        users.load();

        if (firstRun) {
            DemoData.seedUsers(users);
        }

        authService = new AuthService(users, session, auditLog);
        userService = new UserService(users, session, auditLog);
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
}
