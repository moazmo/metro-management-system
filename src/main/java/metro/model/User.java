package metro.model;

import java.util.Set;
import metro.util.Check;

/**
 * A staff account. This class is abstract: a real user is always one of the
 * subclasses (Admin, OperationsController, TicketAgent, StationSupervisor).
 *
 * Each subclass answers two questions in its own way: which role it is, and
 * which permissions it has. The rest of the program only asks
 * user.can(permission) and never needs to know which subclass it is talking
 * to. That is polymorphism.
 *
 * The id (from Entity) and the username have no setters, so they cannot be
 * changed after the account is created.
 */
public abstract class User extends Entity {

    private final String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String phone;
    private String email;
    private boolean active;

    protected User(String id, String username, String passwordHash, String salt,
                   String fullName, String phone, String email, boolean active) {
        super(id);
        this.username = Check.notBlank(username, "Username");
        setPassword(passwordHash, salt);
        setFullName(fullName);
        setPhone(phone);
        setEmail(email);
        this.active = active;
    }

    /** Which role this account has. */
    public abstract Role getRole();

    /** Everything this account is allowed to do. */
    public abstract Set<Permission> getPermissions();

    /** True when this account is allowed to do the given thing. */
    public boolean can(Permission permission) {
        return getPermissions().contains(permission);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    /** Stores a new password. Only the hash and its salt are kept, never the password itself. */
    public void setPassword(String passwordHash, String salt) {
        this.passwordHash = Check.notBlank(passwordHash, "Password hash");
        this.salt = Check.notBlank(salt, "Salt");
    }

    public String getFullName() {
        return fullName;
    }

    /** Rule: an account must have a full name. */
    public void setFullName(String fullName) {
        this.fullName = Check.notBlank(fullName, "Full name");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
