package metro.service;

import metro.model.Role;
import metro.model.UserFactory;
import metro.repository.UserRepository;

/**
 * Creates the data the application starts with on its first run, so that
 * there is something to log in to and something to show.
 */
public final class DemoData {

    /** The password of every demo account. */
    public static final String DEMO_PASSWORD = "metro123";

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
}
