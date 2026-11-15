package metro.model;

import java.util.Set;

/**
 * A station supervisor is a ticket agent who may also record refunds.
 *
 * It extends TicketAgent, so it starts with everything an agent may do
 * (super.getPermissions()) and adds one more permission.
 */
public class StationSupervisor extends TicketAgent {

    public StationSupervisor(String id, String username, String passwordHash, String salt,
                             String fullName, String phone, String email, boolean active) {
        super(id, username, passwordHash, salt, fullName, phone, email, active);
    }

    @Override
    public Role getRole() {
        return Role.STATION_SUPERVISOR;
    }

    @Override
    public Set<Permission> getPermissions() {
        Set<Permission> permissions = super.getPermissions();
        permissions.add(Permission.REFUND_TICKETS);
        return permissions;
    }
}
