package metro.model;

import static metro.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import metro.TestData;
import metro.exception.DataFileException;
import metro.exception.ValidationException;
import metro.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TicketTest {

    @TempDir
    Path dir;
    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
    }

    private Ticket ticket(TicketType type, double baseFare) {
        return TicketFactory.create(type, data.tickets.nextId(), data.stationA, data.stationG, 3, baseFare,
                data.agent.getId(), at(10, 0));
    }

    private TicketRepository reload() {
        TicketRepository fresh = new TicketRepository(dir.resolve("tickets.txt"), data.stations);
        fresh.load();
        return fresh;
    }

    // ----- fares: each type answers getFare() in its own way -----

    @Test
    void eachTypeHasItsOwnFare() {
        Ticket standard = ticket(TicketType.STANDARD, 10.00);
        Ticket student = ticket(TicketType.STUDENT, 10.00);
        Ticket senior = ticket(TicketType.SENIOR, 10.00);
        assertEquals(10.00, standard.getFare());
        assertEquals(5.00, student.getFare());
        assertEquals(7.00, senior.getFare());
    }

    @Test
    void fareIsRoundedToTwoDecimals() {
        assertEquals(10.50, ticket(TicketType.SENIOR, 15.00).getFare());
        assertEquals(4.13, ticket(TicketType.STUDENT, 8.25).getFare());
    }

    @Test
    void factoryBuildsTheRightClass() {
        for (TicketType type : TicketType.values()) {
            assertEquals(type, ticket(type, 8.00).getType());
        }
        assertTrue(ticket(TicketType.STUDENT, 8.00) instanceof StudentTicket);
        assertTrue(ticket(TicketType.SENIOR, 8.00) instanceof SeniorTicket);
        assertTrue(ticket(TicketType.STANDARD, 8.00) instanceof StandardTicket);
    }

    // ----- life of a ticket -----

    @Test
    void newTicketIsValid() {
        Ticket ticket = ticket(TicketType.STANDARD, 8.00);
        assertEquals(TicketStatus.VALID, ticket.getStatus());
        assertNull(ticket.getUsedAt());
        assertNull(ticket.getRefundedAt());
        assertSame(data.stationA, ticket.getFrom());
        assertSame(data.stationG, ticket.getTo());
        assertEquals(3, ticket.getStops());
    }

    @Test
    void usedOnlyOnce() {
        Ticket ticket = ticket(TicketType.STANDARD, 8.00);
        ticket.markUsed(at(10, 30));
        ValidationException e = assertThrows(ValidationException.class, () -> ticket.markUsed(at(11, 0)));
        assertEquals("Ticket is already used", e.getMessage());
        assertEquals(at(10, 30), ticket.getUsedAt());
        assertEquals(TicketStatus.USED, ticket.getStatus());
    }

    @Test
    void usedTicketCannotBeRefunded() {
        Ticket ticket = ticket(TicketType.STANDARD, 8.00);
        ticket.markUsed(at(10, 30));
        ValidationException e = assertThrows(ValidationException.class,
                () -> ticket.refund(data.supervisor.getId(), at(11, 0)));
        assertEquals("Ticket is already used", e.getMessage());
    }

    @Test
    void refundedTicketCannotBeUsedOrRefundedAgain() {
        Ticket ticket = ticket(TicketType.STANDARD, 8.00);
        ticket.refund(data.supervisor.getId(), at(10, 20));
        ValidationException used = assertThrows(ValidationException.class, () -> ticket.markUsed(at(11, 0)));
        assertEquals("Ticket was refunded", used.getMessage());
        ValidationException again = assertThrows(ValidationException.class,
                () -> ticket.refund(data.supervisor.getId(), at(11, 0)));
        assertEquals("Ticket is already refunded", again.getMessage());
    }

    @Test
    void refundRecordsWhoAndWhen() {
        Ticket ticket = ticket(TicketType.STANDARD, 8.00);
        ticket.refund(data.supervisor.getId(), at(10, 20));
        assertEquals(TicketStatus.REFUNDED, ticket.getStatus());
        assertEquals(data.supervisor.getId(), ticket.getRefundedById());
        assertEquals(at(10, 20), ticket.getRefundedAt());
    }

    @Test
    void expiresAtMidnight() {
        Ticket ticket = TicketFactory.create(TicketType.STANDARD, "TKT-000009", data.stationA, data.stationG, 3,
                8.00, data.agent.getId(), at(23, 59));
        assertFalse(ticket.isExpired(LocalDate.of(2026, 11, 15)));
        assertTrue(ticket.isExpired(LocalDate.of(2026, 11, 16)));
    }

    // ----- storage -----

    @Test
    void typeAndStatusSurviveReload() {
        Ticket used = ticket(TicketType.STUDENT, 8.00);
        used.markUsed(at(10, 30));
        Ticket refunded = ticket(TicketType.SENIOR, 15.00);
        refunded.refund(data.supervisor.getId(), at(10, 40));
        data.tickets.save(used);
        data.tickets.save(refunded);

        TicketRepository fresh = reload();
        Ticket loadedUsed = fresh.require(used.getId());
        assertTrue(loadedUsed instanceof StudentTicket);
        assertEquals(TicketStatus.USED, loadedUsed.getStatus());
        assertEquals(at(10, 30), loadedUsed.getUsedAt());
        assertEquals(4.00, loadedUsed.getFare());
        assertEquals(data.agent.getId(), loadedUsed.getSoldById());

        Ticket loadedRefunded = fresh.require(refunded.getId());
        assertTrue(loadedRefunded instanceof SeniorTicket);
        assertEquals(TicketStatus.REFUNDED, loadedRefunded.getStatus());
        assertEquals(data.supervisor.getId(), loadedRefunded.getRefundedById());
        assertEquals(at(10, 40), loadedRefunded.getRefundedAt());
        assertEquals(10.50, loadedRefunded.getFare());
    }

    @Test
    void fileLineLooksRight() {
        data.tickets.save(ticket(TicketType.STUDENT, 8.00));
        assertEquals("TKT-000001|STUDENT|STN-001|STN-007|3|8.0|USR-003|2026-11-15T10:00:00|VALID|||",
                data.lineStarting("tickets.txt", "TKT-000001|"));
    }

    @Test
    void unknownStationNamesFileAndLine() throws IOException {
        Files.write(dir.resolve("tickets.txt"), List.of("#next=2", "# header",
                "TKT-000001|STUDENT|STN-001|STN-999|3|8.0|USR-003|2026-11-15T10:00:00|VALID|||"),
                StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("tickets.txt line 3: unknown station STN-999"), e.getMessage());
    }

    @Test
    void existsForStation() {
        data.tickets.save(ticket(TicketType.STANDARD, 8.00));
        assertTrue(data.tickets.existsForStation(data.stationA));
        assertTrue(data.tickets.existsForStation(data.stationG));
        assertFalse(data.tickets.existsForStation(data.stationB));
    }
}
