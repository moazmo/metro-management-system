package metro.model;

/**
 * Creates the right User subclass for a role. Used when loading users from
 * the file and when an admin creates an account or changes its role.
 */
public final class UserFactory {

    private UserFactory() {
    }

    public static User create(Role role, String id, String username, String passwordHash, String salt,
                              String fullName, String phone, String email, boolean active) {
        switch (role) {
            case ADMIN:
                return new Admin(id, username, passwordHash, salt, fullName, phone, email, active);
            case OPERATIONS_CONTROLLER:
                return new OperationsController(id, username, passwordHash, salt, fullName, phone, email, active);
            case TICKET_AGENT:
                return new TicketAgent(id, username, passwordHash, salt, fullName, phone, email, active);
            case STATION_SUPERVISOR:
                return new StationSupervisor(id, username, passwordHash, salt, fullName, phone, email, active);
            default:
                throw new IllegalArgumentException("Unknown role " + role);
        }
    }
}
