package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Permission;
import metro.model.Role;
import metro.model.StationSupervisor;
import metro.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class UserServiceTest {

    @TempDir
    Path dir;
    TestData data;
    UserService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.userService;
        data.loginAs(data.admin);
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    private User createNour() {
        return service.create(Role.TICKET_AGENT, "nour", "secret1", "Nour Hassan", "0100000000", "nour@metro.eg");
    }

    @Test
    void agentCannotListUsers() {
        data.loginAs(data.agent);
        assertThrows(AccessDeniedException.class, () -> service.listAll());
    }

    @Test
    void nobodyLoggedInIsDenied() {
        data.session.logout();
        assertThrows(AccessDeniedException.class, () -> service.listAll());
    }

    @Test
    void createStoresHashedPassword() {
        User nour = createNour();
        assertNotEquals("secret1", nour.getPasswordHash());
        assertTrue(PasswordHasher.matches("secret1", nour.getSalt(), nour.getPasswordHash()));
    }

    @Test
    void createWritesTheFile() {
        createNour();
        assertNotNull(data.lineStarting("users.txt", "USR-005|nour|"));
    }

    @Test
    void usernameIsStoredInLowerCase() {
        User user = service.create(Role.TICKET_AGENT, " Nour ", "secret1", "Nour Hassan", "", "");
        assertEquals("nour", user.getUsername());
    }

    @Test
    void duplicateUsernameIgnoringCase() {
        assertRefused("Username is already taken",
                () -> service.create(Role.TICKET_AGENT, "Admin", "secret1", "Another Admin", "", ""));
    }

    @Test
    void badUsername() {
        String message = "Username must be 3–20 letters, digits, dots or underscores";
        assertRefused(message, () -> service.create(Role.TICKET_AGENT, "ab", "secret1", "Name", "", ""));
        assertRefused(message, () -> service.create(Role.TICKET_AGENT, "has space", "secret1", "Name", "", ""));
        assertRefused(message, () -> service.create(Role.TICKET_AGENT, "a|b|c", "secret1", "Name", "", ""));
    }

    @Test
    void shortPassword() {
        assertRefused("Password must be at least 6 characters",
                () -> service.create(Role.TICKET_AGENT, "nour", "12345", "Name", "", ""));
    }

    @Test
    void badEmail() {
        assertRefused("Email address is not valid",
                () -> service.create(Role.TICKET_AGENT, "nour", "secret1", "Name", "", "not-an-email"));
        assertRefused("Email address is not valid",
                () -> service.create(Role.TICKET_AGENT, "nour", "secret1", "Name", "", "@metro.eg"));
    }

    @Test
    void badPhone() {
        assertRefused("Phone number is not valid",
                () -> service.create(Role.TICKET_AGENT, "nour", "secret1", "Name", "12ab", ""));
    }

    @Test
    void refusedCreateSavesNothing() {
        assertThrows(ValidationException.class,
                () -> service.create(Role.TICKET_AGENT, "nour", "secret1", " ", "", ""));
        assertEquals(4, data.users.findAll().size());
    }

    @Test
    void profileUpdateKeepsIdAndUsername() {
        data.loginAs(data.agent);
        String id = data.agent.getId();
        User updated = service.updateOwnProfile("New Name", "0100000000", "a@b.c");
        assertEquals(id, updated.getId());
        assertEquals("agent", updated.getUsername());
        assertEquals("New Name", updated.getFullName());
        assertTrue(data.lineStarting("users.txt", id + "|agent|").contains("|New Name|0100000000|a@b.c|"));
    }

    @Test
    void lastAdminCannotBeDeactivated() {
        assertRefused("The last active admin cannot be deactivated or changed",
                () -> service.setActive(data.admin.getId(), false));
        assertTrue(data.admin.isActive());
    }

    @Test
    void lastAdminCannotChangeRole() {
        assertRefused("The last active admin cannot be deactivated or changed",
                () -> service.changeRole(data.admin.getId(), Role.TICKET_AGENT));
    }

    @Test
    void cannotDeactivateYourself() {
        service.create(Role.ADMIN, "admin2", "secret1", "Second Admin", "", "");
        assertRefused("You cannot deactivate your own account",
                () -> service.setActive(data.admin.getId(), false));
    }

    @Test
    void deactivatedUserIsSavedAsInactive() {
        service.setActive(data.agent.getId(), false);
        assertFalse(data.users.require(data.agent.getId()).isActive());
        assertTrue(data.lineStarting("users.txt", data.agent.getId() + "|").endsWith("|false"));
    }

    @Test
    void changeRoleSwapsTheClass() {
        String id = data.agent.getId();
        User changed = service.changeRole(id, Role.STATION_SUPERVISOR);
        assertTrue(data.users.require(id) instanceof StationSupervisor);
        assertEquals(id, changed.getId());
        assertEquals("agent", changed.getUsername());
        assertEquals(data.agent.getFullName(), changed.getFullName());
    }

    @Test
    void changingOwnRoleUpdatesSession() {
        User second = service.create(Role.ADMIN, "admin2", "secret1", "Second Admin", "", "");
        data.loginAs(second);
        service.changeRole(second.getId(), Role.TICKET_AGENT);
        assertFalse(data.session.can(Permission.MANAGE_USERS));
        assertTrue(data.session.can(Permission.SELL_TICKETS));
    }

    @Test
    void changeOwnPasswordNeedsCurrent() {
        data.loginAs(data.agent);
        assertRefused("Current password is wrong", () -> service.changeOwnPassword("nope", "newpass1"));
        service.changeOwnPassword(TestData.PASSWORD, "newpass1");
        data.auth.logout();
        assertSame(data.agent, data.auth.login("agent", "newpass1"));
    }

    @Test
    void resetPasswordLetsUserLogIn() {
        service.resetPassword(data.agent.getId(), "fresh123");
        data.auth.logout();
        assertSame(data.agent, data.auth.login("agent", "fresh123"));
    }

    @Test
    void searchMatchesUsernameNameAndRole() {
        assertEquals(1, service.search("window").size());
        assertEquals(1, service.search("CONTROLLER").size());
        assertEquals(1, service.search("station sup").size());
        assertEquals(4, service.search("  ").size());
    }

    @Test
    void displayNameFallsBackToTheId() {
        assertEquals("Ticket Window 1", service.displayName(data.agent.getId()));
        assertEquals("USR-999", service.displayName("USR-999"));
    }

    @Test
    void passwordsNeverReachTheAuditLog() {
        createNour();
        service.resetPassword(data.agent.getId(), "fresh123");
        for (String line : data.lines("audit.log")) {
            assertFalse(line.contains("secret1") || line.contains("fresh123"), line);
        }
    }
}
