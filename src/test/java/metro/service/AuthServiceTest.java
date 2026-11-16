package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.AuditEntry;
import metro.model.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthServiceTest {

    @TempDir
    Path dir;
    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
    }

    @Test
    void loginPutsUserInSession() {
        assertSame(data.admin, data.auth.login("admin", TestData.PASSWORD));
        assertSame(data.admin, data.session.getUser());
    }

    @Test
    void loginIgnoresCaseAndSpaces() {
        assertSame(data.admin, data.auth.login("  ADMIN ", TestData.PASSWORD));
    }

    @Test
    void wrongPasswordAndUnknownUserLookTheSame() {
        ValidationException wrong = assertThrows(ValidationException.class, () -> data.auth.login("admin", "nope"));
        ValidationException unknown = assertThrows(ValidationException.class, () -> data.auth.login("ghost", "nope"));
        assertEquals("Wrong username or password", wrong.getMessage());
        assertEquals("Wrong username or password", unknown.getMessage());
        assertFalse(data.session.isLoggedIn());
    }

    @Test
    void deactivatedAccountIsRefused() {
        data.agent.setActive(false);
        data.users.save(data.agent);
        ValidationException e = assertThrows(ValidationException.class,
                () -> data.auth.login("agent", TestData.PASSWORD));
        assertEquals("This account is deactivated", e.getMessage());
        assertFalse(data.session.isLoggedIn());
    }

    @Test
    void logoutClearsSession() {
        data.auth.login("admin", TestData.PASSWORD);
        data.auth.logout();
        assertNull(data.session.getUser());
        assertFalse(data.session.isLoggedIn());
    }

    @Test
    void loginAndFailureAreAudited() {
        assertThrows(ValidationException.class, () -> data.auth.login("admin", "nope"));
        data.auth.login("admin", TestData.PASSWORD);
        List<AuditEntry> entries = data.audit.readAll();
        assertEquals(2, entries.size());
        assertEquals("LOGIN_FAILED", entries.get(0).getAction());
        assertEquals("admin", entries.get(0).getUsername());
        assertEquals("LOGIN", entries.get(1).getAction());
        assertEquals("admin", entries.get(1).getUsername());
    }

    @Test
    void sessionRefusesWhatTheRoleMayNot() {
        data.loginAs(data.agent);
        data.session.require(Permission.SELL_TICKETS);
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> data.session.require(Permission.MANAGE_USERS));
        assertEquals("You do not have permission to do this.", e.getMessage());
    }

    @Test
    void sessionRefusesEverythingWhenLoggedOut() {
        assertFalse(data.session.can(Permission.EDIT_OWN_PROFILE));
        assertThrows(AccessDeniedException.class, () -> data.session.require(Permission.EDIT_OWN_PROFILE));
        AccessDeniedException e = assertThrows(AccessDeniedException.class, () -> data.session.requireLogin());
        assertEquals("Please log in first.", e.getMessage());
    }
}
