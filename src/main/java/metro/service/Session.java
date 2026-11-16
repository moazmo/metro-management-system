package metro.service;

import metro.exception.AccessDeniedException;
import metro.model.Permission;
import metro.model.User;

/**
 * Remembers who is logged in, and answers "may this user do that?".
 *
 * Every service keeps a reference to the one Session of the application and
 * calls require(...) at the start of each operation. That is the second line
 * of defence for authorization: even if a screen showed a button by mistake,
 * the operation behind it would still be refused.
 */
public class Session {

    private User user;

    public void login(User user) {
        this.user = user;
    }

    public void logout() {
        this.user = null;
    }

    /** The logged-in user, or null when nobody is logged in. */
    public User getUser() {
        return user;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    /** True when somebody is logged in and their role has the permission. */
    public boolean can(Permission permission) {
        return user != null && user.can(permission);
    }

    /** Rule: an operation is refused unless the logged-in user has its permission. */
    public void require(Permission permission) {
        if (!can(permission)) {
            throw new AccessDeniedException("You do not have permission to do this.");
        }
    }

    /** Rule: an operation open to every role still needs somebody to be logged in. */
    public void requireLogin() {
        if (user == null) {
            throw new AccessDeniedException("Please log in first.");
        }
    }
}
