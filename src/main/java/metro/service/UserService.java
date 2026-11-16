package metro.service;

import java.util.ArrayList;
import java.util.List;
import metro.exception.ValidationException;
import metro.model.Permission;
import metro.model.Role;
import metro.model.User;
import metro.model.UserFactory;
import metro.repository.AuditLog;
import metro.repository.UserRepository;
import metro.util.Check;

/**
 * The rules for staff accounts: creating and changing them (admins only)
 * and editing your own profile (everyone).
 */
public class UserService {

    private static final String LAST_ADMIN = "The last active admin cannot be deactivated or changed";

    private final UserRepository users;
    private final Session session;
    private final AuditLog audit;

    public UserService(UserRepository users, Session session, AuditLog audit) {
        this.users = users;
        this.session = session;
        this.audit = audit;
    }

    public List<User> listAll() {
        session.require(Permission.MANAGE_USERS);
        return users.findAll();
    }

    /** Users whose username, full name or role contains the text. Blank text gives everyone. */
    public List<User> search(String text) {
        session.require(Permission.MANAGE_USERS);
        String wanted = text == null ? "" : text.trim().toLowerCase();
        List<User> found = new ArrayList<>();
        for (User user : users.findAll()) {
            if (wanted.isEmpty()
                    || user.getUsername().toLowerCase().contains(wanted)
                    || user.getFullName().toLowerCase().contains(wanted)
                    || user.getRole().getDisplayName().toLowerCase().contains(wanted)) {
                found.add(user);
            }
        }
        return found;
    }

    /** Creates a new account. Everything is checked before anything is saved. */
    public User create(Role role, String username, String password,
                       String fullName, String phone, String email) {
        session.require(Permission.MANAGE_USERS);
        if (role == null) {
            throw new ValidationException("Role is required");
        }
        String cleanUsername = checkUsername(username);
        if (users.findByUsername(cleanUsername) != null) {
            throw new ValidationException("Username is already taken");
        }
        checkPassword(password);
        String cleanName = Check.notBlank(fullName, "Full name");
        checkPhone(phone);
        checkEmail(email);

        String salt = PasswordHasher.newSalt();
        User user = UserFactory.create(role, users.nextId(), cleanUsername,
                PasswordHasher.hash(password, salt), salt, cleanName, phone, email, true);
        users.save(user);
        record("USER_CREATED", user);
        return user;
    }

    public User update(String id, String fullName, String phone, String email) {
        session.require(Permission.MANAGE_USERS);
        User user = users.require(id);
        applyDetails(user, fullName, phone, email);
        record("USER_UPDATED", user);
        return user;
    }

    /**
     * Gives an account another role. Because each role is its own class, the
     * old object is replaced by a new object of the new class with the same
     * id and details.
     */
    public User changeRole(String id, Role role) {
        session.require(Permission.MANAGE_USERS);
        User old = users.require(id);
        if (role == null) {
            throw new ValidationException("Role is required");
        }
        if (old.getRole() == role) {
            return old;
        }
        if (isLastActiveAdmin(old)) {
            throw new ValidationException(LAST_ADMIN);
        }
        User changed = UserFactory.create(role, old.getId(), old.getUsername(), old.getPasswordHash(),
                old.getSalt(), old.getFullName(), old.getPhone(), old.getEmail(), old.isActive());
        users.save(changed);
        if (old.equals(session.getUser())) {
            session.login(changed); // the logged-in user now has the new role's permissions
        }
        record("USER_ROLE_CHANGED", changed);
        return changed;
    }

    /**
     * Activates or deactivates an account.
     * Rule: the last active admin cannot be deactivated (nobody could manage the system).
     * Rule: you cannot deactivate your own account.
     */
    public User setActive(String id, boolean active) {
        session.require(Permission.MANAGE_USERS);
        User user = users.require(id);
        if (!active) {
            if (isLastActiveAdmin(user)) {
                throw new ValidationException(LAST_ADMIN);
            }
            if (user.equals(session.getUser())) {
                throw new ValidationException("You cannot deactivate your own account");
            }
        }
        user.setActive(active);
        users.save(user);
        record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", user);
        return user;
    }

    public void resetPassword(String id, String newPassword) {
        session.require(Permission.MANAGE_USERS);
        User user = users.require(id);
        storePassword(user, newPassword);
        record("PASSWORD_RESET", user);
    }

    /** The logged-in user changes their own details. The id and username cannot be changed. */
    public User updateOwnProfile(String fullName, String phone, String email) {
        session.require(Permission.EDIT_OWN_PROFILE);
        User user = session.getUser();
        applyDetails(user, fullName, phone, email);
        record("PROFILE_UPDATED", user);
        return user;
    }

    public void changeOwnPassword(String currentPassword, String newPassword) {
        session.require(Permission.EDIT_OWN_PROFILE);
        User user = session.getUser();
        if (currentPassword == null
                || !PasswordHasher.matches(currentPassword, user.getSalt(), user.getPasswordHash())) {
            throw new ValidationException("Current password is wrong");
        }
        storePassword(user, newPassword);
        record("PASSWORD_CHANGED", user);
    }

    /** The full name of a user, or the id itself when no such user exists. */
    public String displayName(String userId) {
        session.requireLogin();
        User user = users.findById(userId);
        return user == null ? userId : user.getFullName();
    }

    // ----- helpers -----

    private void applyDetails(User user, String fullName, String phone, String email) {
        String cleanName = Check.notBlank(fullName, "Full name");
        checkPhone(phone);
        checkEmail(email);
        user.setFullName(cleanName);
        user.setPhone(phone);
        user.setEmail(email);
        users.save(user);
    }

    private void storePassword(User user, String password) {
        checkPassword(password);
        String salt = PasswordHasher.newSalt();
        user.setPassword(PasswordHasher.hash(password, salt), salt);
        users.save(user);
    }

    private boolean isLastActiveAdmin(User user) {
        if (user.getRole() != Role.ADMIN || !user.isActive()) {
            return false;
        }
        int activeAdmins = 0;
        for (User other : users.findAll()) {
            if (other.getRole() == Role.ADMIN && other.isActive()) {
                activeAdmins++;
            }
        }
        return activeAdmins == 1;
    }

    /** Rule: 3 to 20 letters, digits, dots or underscores. Stored in lower case. */
    private String checkUsername(String username) {
        String clean = username == null ? "" : username.trim().toLowerCase();
        if (!clean.matches("[a-z0-9._]{3,20}")) {
            throw new ValidationException("Username must be 3–20 letters, digits, dots or underscores");
        }
        return clean;
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
    }

    /** Rule: an email may be left empty; otherwise it needs text on both sides of an @. */
    private void checkEmail(String email) {
        String clean = email == null ? "" : email.trim();
        if (clean.isEmpty()) {
            return;
        }
        int at = clean.indexOf('@');
        if (at <= 0 || at == clean.length() - 1) {
            throw new ValidationException("Email address is not valid");
        }
    }

    /** Rule: a phone may be left empty; otherwise 7 to 15 digits, spaces or +. */
    private void checkPhone(String phone) {
        String clean = phone == null ? "" : phone.trim();
        if (!clean.isEmpty() && !clean.matches("[0-9 +]{7,15}")) {
            throw new ValidationException("Phone number is not valid");
        }
    }

    private void record(String action, User affected) {
        audit.record(session.getUser().getUsername(), action, affected.getUsername());
    }
}
