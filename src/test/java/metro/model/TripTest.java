package metro.model;

import static metro.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import metro.TestData;
import metro.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TripTest {

    @TempDir
    Path dir;
    TestData data;
    Trip trip;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        trip = new Trip("TRP-0001", data.train1, data.red, Direction.FORWARD, at(10, 0), at(10, 45));
    }

    // ----- status rules -----

    @Test
    void scheduledCanBecomeDelayedCompletedCancelled() {
        assertTrue(TripStatus.SCHEDULED.canChangeTo(TripStatus.DELAYED));
        assertTrue(TripStatus.SCHEDULED.canChangeTo(TripStatus.COMPLETED));
        assertTrue(TripStatus.SCHEDULED.canChangeTo(TripStatus.CANCELLED));
        assertFalse(TripStatus.SCHEDULED.canChangeTo(TripStatus.SCHEDULED));
    }

    @Test
    void delayedCanStayDelayedOrFinish() {
        assertTrue(TripStatus.DELAYED.canChangeTo(TripStatus.DELAYED));
        assertTrue(TripStatus.DELAYED.canChangeTo(TripStatus.COMPLETED));
        assertTrue(TripStatus.DELAYED.canChangeTo(TripStatus.CANCELLED));
        assertFalse(TripStatus.DELAYED.canChangeTo(TripStatus.SCHEDULED));
    }

    @Test
    void finalStatusesCannotChange() {
        for (TripStatus next : TripStatus.values()) {
            assertFalse(TripStatus.COMPLETED.canChangeTo(next));
            assertFalse(TripStatus.CANCELLED.canChangeTo(next));
        }
        assertTrue(TripStatus.COMPLETED.isFinal());
        assertTrue(TripStatus.CANCELLED.isFinal());
        assertFalse(TripStatus.SCHEDULED.isFinal());
        assertFalse(TripStatus.DELAYED.isFinal());
    }

    @Test
    void finishedTripCannotBeReopened() {
        trip.changeStatus(TripStatus.COMPLETED);
        ValidationException e = assertThrows(ValidationException.class,
                () -> trip.changeStatus(TripStatus.SCHEDULED));
        assertEquals("A Completed trip cannot become Scheduled", e.getMessage());
        assertEquals(TripStatus.COMPLETED, trip.getStatus());
    }

    // ----- times -----

    @Test
    void newTripIsScheduledWithNoDelay() {
        assertEquals(TripStatus.SCHEDULED, trip.getStatus());
        assertEquals(0, trip.getDelayMinutes());
        assertEquals(at(10, 45), trip.getEffectiveArrival());
    }

    @Test
    void arrivalMustBeAfterDeparture() {
        ValidationException same = assertThrows(ValidationException.class,
                () -> new Trip("TRP-0002", data.train1, data.red, Direction.FORWARD, at(10, 0), at(10, 0)));
        assertEquals("Arrival must be after departure", same.getMessage());
        assertThrows(ValidationException.class,
                () -> new Trip("TRP-0002", data.train1, data.red, Direction.FORWARD, at(10, 0), at(9, 59)));
        assertThrows(ValidationException.class,
                () -> trip.reschedule(data.train1, data.red, Direction.FORWARD, at(11, 0), at(10, 30)));
        assertEquals(at(10, 0), trip.getDeparture());
    }

    @Test
    void overlapInsideIsDetected() {
        assertTrue(trip.overlaps(at(10, 30), at(11, 0), 0));
    }

    @Test
    void touchingTripsNeedTheGap() {
        assertFalse(trip.overlaps(at(10, 45), at(11, 15), 0));
        assertTrue(trip.overlaps(at(10, 45), at(11, 15), 5));
    }

    @Test
    void outsideTheGapIsFine() {
        assertFalse(trip.overlaps(at(10, 50), at(11, 20), 5));
        assertFalse(trip.overlaps(at(9, 0), at(9, 55), 5));
        assertTrue(trip.overlaps(at(9, 0), at(9, 56), 5));
    }

    @Test
    void delayExtendsTheBusyTime() {
        trip.setDelayMinutes(10);
        assertEquals(at(10, 55), trip.getEffectiveArrival());
        assertTrue(trip.overlaps(at(10, 50), at(11, 20), 5));
    }

    @Test
    void tripAcrossMidnightWorks() {
        LocalDateTime nextDay = at(0, 0).plusDays(1);
        Trip late = new Trip("TRP-0003", data.train1, data.red, Direction.FORWARD, at(23, 50), nextDay.plusMinutes(20));
        assertTrue(late.overlaps(nextDay.plusMinutes(10), nextDay.plusMinutes(40), 0));
        assertFalse(late.overlaps(nextDay.plusMinutes(30), nextDay.plusMinutes(60), 0));
    }

    @Test
    void originAndDestinationFollowDirection() {
        assertSame(data.stationA, trip.getOrigin());
        assertSame(data.stationE, trip.getDestination());
        Trip back = new Trip("TRP-0004", data.train1, data.red, Direction.BACKWARD, at(12, 0), at(12, 45));
        assertSame(data.stationE, back.getOrigin());
        assertSame(data.stationA, back.getDestination());
    }

    @Test
    void delayMustNotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> trip.setDelayMinutes(-1));
    }
}
