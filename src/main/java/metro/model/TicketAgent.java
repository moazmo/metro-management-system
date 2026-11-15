package metro.model;

import java.util.EnumSet;
import java.util.Set;

/** Staff at a ticket window: sells tickets and validates them. */
public class TicketAgent extends User {

    public TicketAgent(String id, String username, String passwordHash, String salt,
                       String fullName, String phone, String email, boolean active) {
        super(id, username, passwordHash, salt, fullName, phone, email, active);
    }

    @Override
    public Role getRole() {
        return Role.TICKET_AGENT;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(
                Permission.VIEW_TRIPS,
                Permission.VIEW_NETWORK,
                Permission.SELL_TICKETS,
                Permission.VALIDATE_TICKETS,
                Permission.EDIT_OWN_PROFILE);
    }
}
