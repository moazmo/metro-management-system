package metro.model;

import java.util.EnumSet;
import java.util.Set;

/** An administrator: may do everything. */
public class Admin extends User {

    public Admin(String id, String username, String passwordHash, String salt,
                 String fullName, String phone, String email, boolean active) {
        super(id, username, passwordHash, salt, fullName, phone, email, active);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.allOf(Permission.class);
    }
}
