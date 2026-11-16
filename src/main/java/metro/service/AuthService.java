package metro.service;

import metro.exception.ValidationException;
import metro.model.User;
import metro.repository.AuditLog;
import metro.repository.UserRepository;

/**
 * Logging in and out.
 */
public class AuthService {

    private final UserRepository users;
    private final Session session;
    private final AuditLog audit;

    public AuthService(UserRepository users, Session session, AuditLog audit) {
        this.users = users;
        this.session = session;
        this.audit = audit;
    }

    /**
     * Checks the username and password and, when they are right, puts the
     * user into the session.
     *
     * Rule: an unknown username and a wrong password give the same message,
     * so nobody can find out which usernames exist.
     * Rule: a deactivated account cannot log in.
     */
    public User login(String username, String password) {
        String typed = username == null ? "" : username.trim();
        User user = users.findByUsername(typed);
        boolean passwordRight = user != null && password != null
                && PasswordHasher.matches(password, user.getSalt(), user.getPasswordHash());
        if (!passwordRight) {
            audit.record(typed, "LOGIN_FAILED", "wrong username or password");
            throw new ValidationException("Wrong username or password");
        }
        if (!user.isActive()) {
            audit.record(user.getUsername(), "LOGIN_FAILED", "account is deactivated");
            throw new ValidationException("This account is deactivated");
        }
        session.login(user);
        audit.record(user.getUsername(), "LOGIN", "");
        return user;
    }

    public void logout() {
        if (session.isLoggedIn()) {
            audit.record(session.getUser().getUsername(), "LOGOUT", "");
        }
        session.logout();
    }
}
