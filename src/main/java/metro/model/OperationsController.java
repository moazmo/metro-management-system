package metro.model;

import java.util.EnumSet;
import java.util.Set;

/** Staff who plan and control the train timetable. */
public class OperationsController extends User {

    public OperationsController(String id, String username, String passwordHash, String salt,
                                String fullName, String phone, String email, boolean active) {
        super(id, username, passwordHash, salt, fullName, phone, email, active);
    }

    @Override
    public Role getRole() {
        return Role.OPERATIONS_CONTROLLER;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(
                Permission.MANAGE_TRIPS,
                Permission.VIEW_TRIPS,
                Permission.VIEW_NETWORK,
                Permission.EDIT_OWN_PROFILE);
    }
}
