package metro.service;

import static metro.TestData.at;
import static metro.TestData.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Journey;
import metro.model.StationStatus;
import metro.model.Ticket;
import metro.model.TicketCheck;
import metro.model.TicketStatus;
import metro.model.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/** Clock starts at 2026-11-15 10:00. A to G is 3 stops: base fare 8.00, student fare 4.00. */
class TicketServiceTest {

    @TempDir
    Path dir;
    TestData data;
    TicketService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.ticketService;
        data.loginAs(data.agent);
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    private Ticket sellOne(TicketType type) {
        return service.sell(data.stationA, data.stationG, type, 1).get(0);
    }

    // ----- selling -----

    @Test
    void sellCreatesOneRecordPerTicket() {
        List<Ticket> sold = service.sell(data.stationA, data.stationG, TicketType.STUDENT, 3);
        assertEquals(3, sold.size());
        assertEquals("TKT-000001", sold.get(0).getId());
        assertEquals("TKT-000002", sold.get(1).getId());
        assertEquals("TKT-000003", sold.get(2).getId());
        for (Ticket ticket : sold) {
            assertEquals(3, ticket.getStops());
            assertEquals(8.00, ticket.getBaseFare());
            assertEquals(4.00, ticket.getFare());
            assertEquals(data.agent.getId(), ticket.getSoldById());
            assertEquals(at(10, 0), ticket.getSoldAt());
            assertEquals(TicketStatus.VALID, ticket.getStatus());
        }
        int records = 0;
        for (String line : data.lines("tickets.txt")) {
            if (line.startsWith("TKT-")) {
                records++;
            }
        }
        assertEquals(3, records);
    }

    @Test
    void quoteMatchesWhatIsSold() {
        Journey journey = service.planJourney(data.stationA, data.stationG);
        assertEquals(4.00, service.quoteFare(journey, TicketType.STUDENT));
        assertEquals(8.00, service.quoteFare(journey, TicketType.STANDARD));
        assertEquals(5.60, service.quoteFare(journey, TicketType.SENIOR));
        assertEquals(service.quoteFare(journey, TicketType.SENIOR), sellOne(TicketType.SENIOR).getFare());
        assertEquals("TKT-000001", data.tickets.findAll().get(0).getId()); // a quote uses up no ticket number
    }

    @Test
    void closedStationIsRefused() {
        data.loginAs(data.admin);
        data.stationService.update(data.stationG.getId(), "G", "", StationStatus.CLOSED);
        data.loginAs(data.agent);
        assertRefused("G is closed", () -> service.sell(data.stationA, data.stationG, TicketType.STANDARD, 1));
        assertRefused("G is closed", () -> service.planJourney(data.stationG, data.stationA));
        assertTrue(data.tickets.findAll().isEmpty());
    }

    @Test
    void noRouteIsRefused() {
        assertRefused("No route between A and X",
                () -> service.sell(data.stationA, data.stationX, TicketType.STANDARD, 1));
        assertTrue(data.tickets.findAll().isEmpty());
    }

    @Test
    void quantityLimits() {
        assertRefused("Quantity must be between 1 and 20",
                () -> service.sell(data.stationA, data.stationG, TicketType.STANDARD, 0));
        assertRefused("Quantity must be between 1 and 20",
                () -> service.sell(data.stationA, data.stationG, TicketType.STANDARD, 21));
        assertEquals(20, service.sell(data.stationA, data.stationG, TicketType.STANDARD, 20).size());
    }

    @Test
    void controllerCanPreviewButNotSell() {
        data.loginAs(data.controller);
        Journey journey = service.previewJourney(data.stationA, data.stationG);
        assertEquals(3, journey.getStops());
        assertEquals(8.00, service.quoteFare(journey, TicketType.STANDARD));
        assertThrows(AccessDeniedException.class,
                () -> service.sell(data.stationA, data.stationG, TicketType.STANDARD, 1));
        assertThrows(AccessDeniedException.class, () -> service.planJourney(data.stationA, data.stationG));
    }

    @Test
    void mySalesTodayShowsOnlyMineNewestFirst() {
        Ticket first = sellOne(TicketType.STANDARD);
        data.clock.plusMinutes(5);
        Ticket second = sellOne(TicketType.STUDENT);
        data.loginAs(data.supervisor);
        sellOne(TicketType.STANDARD);
        data.loginAs(data.agent);
        assertEquals(List.of(second, first), service.mySalesToday());
        data.clock.set(on(16, 9, 0));
        assertTrue(service.mySalesToday().isEmpty());
    }

    // ----- validating -----

    @Test
    void validTicketIsAcceptedOnce() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        TicketCheck first = service.validate(ticket.getId());
        assertTrue(first.isAccepted());
        assertEquals("Valid — marked as used", first.getMessage());
        assertSame(ticket, first.getTicket());
        assertEquals(TicketStatus.USED, ticket.getStatus());
        assertEquals(at(10, 0), ticket.getUsedAt());
        assertTrue(data.lineStarting("tickets.txt", ticket.getId() + "|").contains("|USED|2026-11-15T10:00:00|"));

        TicketCheck second = service.validate(ticket.getId());
        assertFalse(second.isAccepted());
        assertEquals("Already used at 10:00 on 15 Nov 2026", second.getMessage());
    }

    @Test
    void inspectChangesNothing() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        TicketCheck check = service.inspect(ticket.getId());
        assertTrue(check.isAccepted());
        assertEquals("Valid", check.getMessage());
        assertEquals(TicketStatus.VALID, ticket.getStatus());
    }

    @Test
    void idIsFoundDespiteCaseAndSpaces() {
        sellOne(TicketType.STANDARD);
        assertTrue(service.inspect("  tkt-000001 ").isAccepted());
    }

    @Test
    void unknownTicket() {
        TicketCheck check = service.inspect("TKT-999999");
        assertFalse(check.isAccepted());
        assertEquals("Ticket not found", check.getMessage());
        assertNull(check.getTicket());
        assertEquals("Ticket not found", service.validate("").getMessage());
    }

    @Test
    void ticketExpiresAtMidnight() {
        data.clock.set(at(23, 59));
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.clock.set(on(16, 0, 1));
        TicketCheck check = service.validate(ticket.getId());
        assertFalse(check.isAccepted());
        assertEquals("Expired — sold on 15 Nov 2026", check.getMessage());
        assertEquals(TicketStatus.VALID, ticket.getStatus());
    }

    @Test
    void controllerCannotValidate() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.loginAs(data.controller);
        assertThrows(AccessDeniedException.class, () -> service.validate(ticket.getId()));
        assertThrows(AccessDeniedException.class, () -> service.inspect(ticket.getId()));
    }

    // ----- refunding -----

    @Test
    void agentCannotRefund() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        assertThrows(AccessDeniedException.class, () -> service.refund(ticket.getId()));
        assertThrows(AccessDeniedException.class, () -> service.refundCheck(ticket.getId()));
    }

    @Test
    void supervisorRefundsSameDay() {
        Ticket ticket = sellOne(TicketType.STUDENT);
        data.loginAs(data.supervisor);
        data.clock.plusMinutes(20);
        TicketCheck check = service.refundCheck(ticket.getId());
        assertTrue(check.isAccepted());
        assertEquals("Eligible for a refund of EGP 4.00", check.getMessage());

        Ticket refunded = service.refund(ticket.getId());
        assertSame(ticket, refunded);
        assertEquals(TicketStatus.REFUNDED, ticket.getStatus());
        assertEquals(data.supervisor.getId(), ticket.getRefundedById());
        assertEquals(at(10, 20), ticket.getRefundedAt());
        assertTrue(data.lineStarting("tickets.txt", ticket.getId() + "|")
                .endsWith("|REFUNDED||USR-004|2026-11-15T10:20:00"));
    }

    @Test
    void refundedTicketIsNotValid() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.loginAs(data.supervisor);
        service.refund(ticket.getId());
        assertEquals("Refunded — not valid for travel", service.validate(ticket.getId()).getMessage());
    }

    @Test
    void refundTwiceIsRefused() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.loginAs(data.supervisor);
        service.refund(ticket.getId());
        assertRefused("Already refunded", () -> service.refund(ticket.getId()));
    }

    @Test
    void usedTicketIsNotRefundable() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        service.validate(ticket.getId());
        data.loginAs(data.supervisor);
        assertFalse(service.refundCheck(ticket.getId()).isAccepted());
        assertRefused("Already used — cannot be refunded", () -> service.refund(ticket.getId()));
    }

    @Test
    void yesterdaysTicketIsNotRefundable() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.loginAs(data.supervisor);
        data.clock.set(on(16, 9, 0));
        assertRefused("Sold on 15 Nov 2026 — refunds are same-day only", () -> service.refund(ticket.getId()));
        assertEquals(TicketStatus.VALID, ticket.getStatus());
    }

    @Test
    void refundOfUnknownTicket() {
        data.loginAs(data.supervisor);
        assertRefused("Ticket not found", () -> service.refund("TKT-424242"));
    }

    // ----- history survives edits -----

    @Test
    void oldTicketsSurviveANetworkEdit() {
        Ticket ticket = sellOne(TicketType.STANDARD);
        data.loginAs(data.admin);
        data.lineService.update(data.blue.getId(), "Blue", "#1976D2",
                List.of(data.stationF, data.stationC, data.stationH));
        data.fareService.updateTier(data.fareService.listTiers().get(0).getId(), 9, 50.0);

        Ticket loaded = data.reloadedTickets().require(ticket.getId());
        assertEquals("G", loaded.getTo().getName());
        assertEquals(3, loaded.getStops());
        assertEquals(8.00, loaded.getFare());
    }

    @Test
    void salesAndChecksAreAudited() {
        service.sell(data.stationA, data.stationG, TicketType.STANDARD, 2);
        service.validate("TKT-000001");
        service.validate("TKT-000001");
        List<String> log = data.lines("audit.log");
        assertTrue(log.get(log.size() - 3).contains("|agent|TICKET_SOLD|TKT-000001…TKT-000002 A → G x2"));
        assertTrue(log.get(log.size() - 2).contains("|agent|TICKET_USED|TKT-000001"));
        assertTrue(log.get(log.size() - 1).contains("|agent|TICKET_REJECTED|TKT-000001: Already used"));
    }
}
