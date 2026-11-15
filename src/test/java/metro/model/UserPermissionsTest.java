package metro.model;

import static metro.model.Permission.EDIT_OWN_PROFILE;
import static metro.model.Permission.MANAGE_TRIPS;
import static metro.model.Permission.REFUND_TICKETS;
import static metro.model.Permission.SELL_TICKETS;
import static metro.model.Permission.VALIDATE_TICKETS;
import static metro.model.Permission.VIEW_NETWORK;
import static metro.model.Permission.VIEW_TRIPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserPermissionsTest {

    private User user(Role role) {
        return UserFactory.create(role, "USR-001", "someone", "hash", "salt", "Some One", "", "", true);
    }

    @Test
    void adminHasEverything() {
        assertEquals(EnumSet.allOf(Permission.class), user(Role.ADMIN).getPermissions());
    }

    @Test
    void controllerPermissions() {
        assertEquals(
                EnumSet.of(MANAGE_TRIPS, VIEW_TRIPS, VIEW_NETWORK, EDIT_OWN_PROFILE),
                user(Role.OPERATIONS_CONTROLLER).getPermissions());
    }

    @Test
    void agentPermissions() {
        assertEquals(
                EnumSet.of(VIEW_TRIPS, VIEW_NETWORK, SELL_TICKETS, VALIDATE_TICKETS, EDIT_OWN_PROFILE),
                user(Role.TICKET_AGENT).getPermissions());
    }

    @Test
    void supervisorIsAgentPlusRefund() {
        User supervisor = user(Role.STATION_SUPERVISOR);
        Set<Permission> expected = EnumSet.copyOf(user(Role.TICKET_AGENT).getPermissions());
        expected.add(REFUND_TICKETS);
        assertEquals(expected, supervisor.getPermissions());
        assertTrue(supervisor instanceof TicketAgent);
    }

    @Test
    void agentCannotRefund() {
        assertFalse(user(Role.TICKET_AGENT).can(REFUND_TICKETS));
        assertTrue(user(Role.STATION_SUPERVISOR).can(REFUND_TICKETS));
    }

    @Test
    void factoryBuildsTheRightClass() {
        for (Role role : Role.values()) {
            assertEquals(role, user(role).getRole());
        }
        assertTrue(user(Role.ADMIN) instanceof Admin);
        assertTrue(user(Role.OPERATIONS_CONTROLLER) instanceof OperationsController);
    }

    @Test
    void permissionsCannotBeChangedFromOutside() {
        User agent = user(Role.TICKET_AGENT);
        try {
            agent.getPermissions().add(REFUND_TICKETS);
        } catch (UnsupportedOperationException e) {
            // an unmodifiable set is one acceptable way to protect it
        }
        assertFalse(agent.can(REFUND_TICKETS));
    }
}
