package metro.service;

import static metro.TestData.at;
import static metro.TestData.on;
import static metro.model.Direction.BACKWARD;
import static metro.model.Direction.FORWARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ScheduleConflictException;
import metro.exception.ValidationException;
import metro.model.Trip;
import metro.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class TripServiceTest {

    @TempDir
    Path dir;
    TestData data;
    TripService service;
    /** train1 on Red, forward, 12:00–12:45 today. */
    Trip base;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.tripService;
        data.loginAs(data.controller);
        base = service.create(data.train1, data.red, FORWARD, at(12, 0), at(12, 45));
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    @Test
    void createSavesAScheduledTrip() {
        assertEquals("TRP-0001", base.getId());
        assertEquals(TripStatus.SCHEDULED, base.getStatus());
        assertEquals("TRP-0001|TRN-001|LIN-01|FORWARD|2026-11-15T12:00:00|2026-11-15T12:45:00|SCHEDULED|0",
                data.lineStarting("trips.txt", "TRP-0001|"));
    }

    @Test
    void suggestedArrivalUsesStops() {
        // Red has 5 stations = 4 hops of 3 minutes
        assertEquals(at(12, 12), service.suggestArrival(data.red, at(12, 0)));
    }

    @Test
    void overlappingTripIsRefused() {
        ScheduleConflictException e = assertThrows(ScheduleConflictException.class,
                () -> service.create(data.train1, data.blue, FORWARD, at(12, 30), at(13, 0)));
        assertEquals("Train T-101 is already on trip TRP-0001 (12:00–12:45, Red). Pick another train or time.",
                e.getMessage());
        assertEquals(1, e.getConflicts().size());
        assertSame(base, e.getConflicts().get(0));
        assertEquals(1, data.trips.findAll().size());
    }

    @Test
    void insideTurnaroundGapIsRefused() {
        assertThrows(ScheduleConflictException.class,
                () -> service.create(data.train1, data.red, BACKWARD, at(12, 48), at(13, 30)));
    }

    @Test
    void afterTheGapIsAccepted() {
        Trip next = service.create(data.train1, data.red, BACKWARD, at(12, 50), at(13, 30));
        assertEquals("TRP-0002", next.getId());
    }

    @Test
    void anotherTrainIsNotAConflict() {
        service.create(data.train2, data.red, FORWARD, at(12, 0), at(12, 45));
        assertEquals(2, data.trips.findAll().size());
    }

    @Test
    void cancelledTripDoesNotBlock() {
        service.cancel(base.getId());
        service.create(data.train1, data.red, FORWARD, at(12, 10), at(12, 55));
        assertEquals(2, data.trips.findAll().size());
    }

    @Test
    void editingATripDoesNotClashWithItself() {
        service.update(base.getId(), data.train1, data.red, FORWARD, at(12, 5), at(12, 50));
        assertEquals(at(12, 5), base.getDeparture());
        assertTrue(data.lineStarting("trips.txt", "TRP-0001|").contains("|2026-11-15T12:05:00|"));
    }

    @Test
    void editIntoAnotherTripIsRefused() {
        service.create(data.train1, data.red, BACKWARD, at(14, 0), at(14, 45));
        assertThrows(ScheduleConflictException.class,
                () -> service.update(base.getId(), data.train1, data.red, FORWARD, at(14, 10), at(14, 50)));
        assertEquals(at(12, 0), base.getDeparture());
        assertEquals(at(12, 45), base.getArrival());
    }

    @Test
    void maintenanceTrainCannotBeScheduled() {
        assertRefused("Train T-103 is In maintenance and cannot be scheduled",
                () -> service.create(data.train3, data.red, FORWARD, at(15, 0), at(15, 45)));
    }

    @Test
    void arrivalBeforeDepartureIsRefusedAndNothingIsSaved() {
        assertRefused("Arrival must be after departure",
                () -> service.create(data.train2, data.red, FORWARD, at(15, 0), at(14, 0)));
        assertEquals(1, data.trips.findAll().size());
        assertEquals("TRP-0002", data.trips.nextId());
    }

    @Test
    void tripAcrossMidnightIsScheduledAndChecked() {
        service.create(data.train2, data.red, FORWARD, at(23, 50), on(16, 0, 20));
        assertThrows(ScheduleConflictException.class,
                () -> service.create(data.train2, data.red, BACKWARD, on(16, 0, 10), on(16, 0, 40)));
    }

    @Test
    void delayIsSavedAndReportsKnockOn() {
        Trip next = service.create(data.train1, data.red, BACKWARD, at(12, 55), at(13, 40));
        List<Trip> clashes = service.delay(base.getId(), 10);
        assertEquals(List.of(next), clashes);
        assertEquals(TripStatus.DELAYED, base.getStatus());
        assertEquals(10, base.getDelayMinutes());
        assertTrue(data.lineStarting("trips.txt", "TRP-0001|").endsWith("|DELAYED|10"));
    }

    @Test
    void delayWithoutKnockOnReturnsEmpty() {
        assertTrue(service.delay(base.getId(), 10).isEmpty());
    }

    @Test
    void aNewDelayReplacesTheOldOne() {
        service.delay(base.getId(), 10);
        service.delay(base.getId(), 25);
        assertEquals(25, base.getDelayMinutes());
        assertEquals(at(13, 10), base.getEffectiveArrival());
    }

    @Test
    void delayRange() {
        assertRefused("Delay must be between 1 and 240 minutes", () -> service.delay(base.getId(), 0));
        assertRefused("Delay must be between 1 and 240 minutes", () -> service.delay(base.getId(), 241));
        assertEquals(TripStatus.SCHEDULED, base.getStatus());
    }

    @Test
    void finishedTripCannotBeEdited() {
        service.complete(base.getId());
        assertRefused("A Completed trip cannot be edited",
                () -> service.update(base.getId(), data.train1, data.red, FORWARD, at(13, 0), at(13, 45)));
    }

    @Test
    void cancelThenCompleteIsRefused() {
        service.cancel(base.getId());
        assertRefused("A Cancelled trip cannot become Completed", () -> service.complete(base.getId()));
    }

    @Test
    void findFiltersByDateLineStatusAndText() {
        service.create(data.train2, data.blue, FORWARD, on(14, 9, 0), on(14, 9, 30));
        Trip cancelledRed = service.create(data.train2, data.red, FORWARD, on(16, 9, 0), on(16, 9, 30));
        service.cancel(cancelledRed.getId());
        LocalDate d14 = LocalDate.of(2026, 11, 14);
        LocalDate d15 = LocalDate.of(2026, 11, 15);
        LocalDate d16 = LocalDate.of(2026, 11, 16);

        assertEquals(List.of(base), service.find(null, d15, d15, null, null));
        assertEquals(1, service.find(null, null, null, data.blue, null).size());
        assertEquals(List.of(cancelledRed), service.find(null, d14, d16, data.red, TripStatus.CANCELLED));
        assertEquals(2, service.find("t-102", null, null, null, null).size());
        assertEquals(3, service.find("  ", null, null, null, null).size());
        assertEquals(2, service.find(null, d15, d16, null, null).size());
    }

    @Test
    void listIsOrderedByDeparture() {
        Trip earlier = service.create(data.train2, data.red, FORWARD, at(8, 0), at(8, 45));
        List<Trip> all = service.listAll();
        assertSame(earlier, all.get(0));
        assertSame(base, all.get(1));
    }

    @Test
    void agentCanViewButNotCreate() {
        data.loginAs(data.agent);
        assertEquals(1, service.listAll().size());
        assertThrows(AccessDeniedException.class,
                () -> service.create(data.train2, data.red, FORWARD, at(15, 0), at(15, 45)));
        assertThrows(AccessDeniedException.class, () -> service.cancel(base.getId()));
    }

    @Test
    void changesAreAudited() {
        service.delay(base.getId(), 5);
        List<String> log = data.lines("audit.log");
        assertTrue(log.get(log.size() - 2).contains("|controller|TRIP_CREATED|TRP-0001 Red 12:00"));
        assertTrue(log.get(log.size() - 1).contains("|controller|TRIP_DELAYED|TRP-0001 Red 12:00"));
    }
}
