package metro.service;

import static metro.TestData.at;
import static metro.TestData.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.model.Direction;
import metro.model.Line;
import metro.model.Ticket;
import metro.model.TicketType;
import metro.model.Train;
import metro.model.Trip;
import metro.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The clock is at 2026-11-15 10:00. */
class DashboardServiceTest {

    @TempDir
    Path dir;
    TestData data;
    DashboardService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.dashboardService;
        data.loginAs(data.admin);
    }

    private Trip trip(Train train, Line line, LocalDateTime departure, TripStatus status) {
        Trip trip = new Trip(data.trips.nextId(), train, line, Direction.FORWARD,
                departure, departure.plusMinutes(20), status, status == TripStatus.DELAYED ? 5 : 0);
        data.trips.save(trip);
        return trip;
    }

    @Test
    void countsTodayOnly() {
        trip(data.train1, data.red, on(14, 9, 0), TripStatus.COMPLETED);
        trip(data.train1, data.red, at(11, 0), TripStatus.CANCELLED);
        trip(data.train2, data.blue, at(12, 0), TripStatus.DELAYED);
        trip(data.train1, data.red, at(13, 0), TripStatus.SCHEDULED);
        trip(data.train1, data.red, on(16, 9, 0), TripStatus.SCHEDULED);
        assertEquals(2, service.tripsToday());
        assertEquals(1, service.delayedToday());
    }

    @Test
    void nextDeparturesAreUpcomingAndOrdered() {
        trip(data.train1, data.red, at(9, 0), TripStatus.SCHEDULED);
        Trip eleven = trip(data.train1, data.red, at(11, 0), TripStatus.SCHEDULED);
        Trip halfTen = trip(data.train2, data.blue, at(10, 30), TripStatus.DELAYED);
        trip(data.train2, data.blue, at(12, 0), TripStatus.CANCELLED);
        assertEquals(List.of(halfTen, eleven), service.nextDepartures(8));
        assertEquals(List.of(halfTen), service.nextDepartures(1));
    }

    @Test
    void revenueIsSalesMinusRefunds() {
        data.loginAs(data.agent);
        List<Ticket> sold = data.ticketService.sell(data.stationA, data.stationG, TicketType.STANDARD, 3);
        data.loginAs(data.supervisor);
        data.ticketService.refund(sold.get(0).getId());
        data.loginAs(data.admin);
        assertEquals(3, service.ticketsSoldToday());
        assertEquals(16.00, service.revenueToday());
    }

    @Test
    void agentSeesOwnSalesButNotRevenue() {
        data.loginAs(data.agent);
        data.ticketService.sell(data.stationA, data.stationG, TicketType.STANDARD, 2);
        assertEquals(2, service.mySalesToday());
        assertThrows(AccessDeniedException.class, () -> service.revenueToday());
        assertThrows(AccessDeniedException.class, () -> service.ticketsSoldToday());
    }

    @Test
    void controllerCannotSeeSales() {
        data.loginAs(data.controller);
        assertEquals(0, service.tripsToday());
        assertThrows(AccessDeniedException.class, () -> service.mySalesToday());
    }

    @Test
    void emptyDayIsZeros() {
        assertEquals(0, service.tripsToday());
        assertEquals(0, service.delayedToday());
        assertTrue(service.nextDepartures(8).isEmpty());
        assertEquals(0, service.ticketsSoldToday());
        assertEquals(0.00, service.revenueToday());
    }
}
