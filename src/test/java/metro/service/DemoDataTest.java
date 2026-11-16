package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import metro.AppContext;
import metro.MutableClock;
import metro.model.Line;
import metro.model.Station;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.model.Trip;
import metro.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the demo data a fresh installation starts with. */
class DemoDataTest {

    @TempDir
    Path dir;
    MutableClock clock = new MutableClock(LocalDateTime.of(2026, 11, 15, 10, 0));
    AppContext context;

    @BeforeEach
    void setUp() {
        context = new AppContext(dir, clock);
        context.getAuthService().login("admin", DemoData.DEMO_PASSWORD);
    }

    private Line line(String name) {
        for (Line line : context.getLineService().listAll()) {
            if (line.getName().equals(name)) {
                return line;
            }
        }
        throw new AssertionError("no line named " + name);
    }

    private Station station(String name) {
        for (Station station : context.getStationService().listAll()) {
            if (station.getName().equals(name)) {
                return station;
            }
        }
        throw new AssertionError("no station named " + name);
    }

    private void assertOn(String stationName, String... lineNames) {
        for (String lineName : lineNames) {
            assertTrue(line(lineName).contains(station(stationName)), stationName + " should be on " + lineName);
        }
    }

    @Test
    void fourLines() {
        assertEquals(4, context.getLineService().listAll().size());
    }

    @Test
    void everyLineHasAtLeastTenStations() {
        for (Line line : context.getLineService().listAll()) {
            assertTrue(line.size() >= 10, line.getName());
        }
    }

    @Test
    void stationNamesAreUnique() {
        Set<String> seen = new HashSet<>();
        for (Station station : context.getStationService().listAll()) {
            assertTrue(seen.add(station.getName().toLowerCase()), "duplicate " + station.getName());
        }
        assertEquals(84, seen.size());
    }

    @Test
    void everyStationHasAnArabicName() {
        for (Station station : context.getStationService().listAll()) {
            assertFalse(station.getNameAr().isBlank(), station.getName());
        }
    }

    @Test
    void interchangesAreShared() {
        assertOn("Sadat", "Line 1", "Line 2");
        assertOn("Al-Shohadaa", "Line 1", "Line 2");
        assertOn("Attaba", "Line 2", "Line 3");
        assertOn("Nasser", "Line 1", "Line 3");
        assertOn("Kit Kat", "Line 3", "Line 3 – Cairo University branch");
        assertOn("Cairo University", "Line 2", "Line 3 – Cairo University branch");
        assertTrue(context.getStationService().isInterchange(station("Sadat")));
        assertFalse(context.getStationService().isInterchange(station("Helwan")));
    }

    @Test
    void helwanIsATerminalOfLineOne() {
        Line one = line("Line 1");
        assertEquals("Helwan", one.getFirstStation().getName());
        assertEquals("New El-Marg", one.getLastStation().getName());
        assertEquals(35, one.size());
        assertEquals("حلوان", one.getFirstStation().getNameAr());
    }

    @Test
    void branchSharesTheTrunkOfLineThree() {
        Line main = line("Line 3");
        Line branch = line("Line 3 – Cairo University branch");
        assertEquals(29, main.size());
        assertEquals(28, branch.size());
        for (int i = 0; i <= main.indexOf(station("Kit Kat")); i++) {
            assertEquals(main.getStations().get(i), branch.getStations().get(i));
        }
    }

    @Test
    void twelveTrainsOneInMaintenance() {
        List<Train> trains = context.getTrainService().listAll();
        assertEquals(12, trains.size());
        int inMaintenance = 0;
        for (Train train : trains) {
            if (train.getStatus() == TrainStatus.MAINTENANCE) {
                inMaintenance++;
                assertEquals("T-112", train.getCode());
            }
        }
        assertEquals(1, inMaintenance);
        assertNotNull(context.getTrainService().search("T-101").get(0));
    }

    @Test
    void seedingTwiceDoesNotDuplicate() {
        AppContext second = new AppContext(dir, clock);
        second.getAuthService().login("admin", DemoData.DEMO_PASSWORD);
        assertEquals(84, second.getStationService().listAll().size());
        assertEquals(4, second.getLineService().listAll().size());
        assertEquals(12, second.getTrainService().listAll().size());
    }

    // ----- demo timetable (Task 14) -----

    private List<Trip> trips() {
        return context.getTripService().listAll();
    }

    @Test
    void demoTripsCoverSevenDays() {
        Set<LocalDate> days = new HashSet<>();
        for (Trip trip : trips()) {
            days.add(trip.getDeparture().toLocalDate());
        }
        LocalDate today = LocalDate.of(2026, 11, 15);
        for (int offset = -3; offset <= 3; offset++) {
            assertTrue(days.contains(today.plusDays(offset)), "no trips on day " + offset);
        }
        assertEquals(7, days.size());
        // 4 lines, 2 directions, 6 departures a day
        assertEquals(7 * 4 * 2 * 6, trips().size());
    }

    @Test
    void demoTripsHaveNoConflicts() {
        List<Trip> all = trips();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Trip a = all.get(i);
                Trip b = all.get(j);
                if (a.getTrain().equals(b.getTrain())
                        && a.getStatus() != TripStatus.CANCELLED && b.getStatus() != TripStatus.CANCELLED) {
                    assertFalse(a.overlaps(b.getDeparture(), b.getEffectiveArrival(),
                            TripService.TURNAROUND_GAP_MINUTES), a.getId() + " clashes with " + b.getId());
                }
            }
        }
    }

    @Test
    void onlyActiveTrainsRunDemoTrips() {
        for (Trip trip : trips()) {
            assertTrue(trip.getTrain().isActive(), trip.getId());
        }
    }

    @Test
    void pastTripsAreFinished() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        int cancelled = 0;
        for (Trip trip : trips()) {
            if (trip.getDeparture().toLocalDate().isBefore(today)) {
                assertTrue(trip.getStatus().isFinal(), trip.getId() + " is " + trip.getStatus());
                if (trip.getStatus() == TripStatus.CANCELLED) {
                    cancelled++;
                }
            }
            if (trip.getDeparture().toLocalDate().isAfter(today)) {
                assertEquals(TripStatus.SCHEDULED, trip.getStatus());
            }
        }
        assertEquals(3, cancelled);
    }

    @Test
    void todayHasTwoDelayedTripsAndFinishedMorningTrips() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        int delayed = 0;
        for (Trip trip : trips()) {
            if (!trip.getDeparture().toLocalDate().equals(today)) {
                assertTrue(trip.getStatus() != TripStatus.DELAYED);
                continue;
            }
            if (trip.getStatus() == TripStatus.DELAYED) {
                delayed++;
                assertTrue(trip.getDelayMinutes() == 10 || trip.getDelayMinutes() == 15);
            } else if (trip.getEffectiveArrival().isBefore(clock.instant().atZone(clock.getZone()).toLocalDateTime())) {
                assertEquals(TripStatus.COMPLETED, trip.getStatus(), trip.getId());
            }
        }
        assertEquals(2, delayed);
    }

    @Test
    void seedingIsRepeatable(@TempDir Path other) throws java.io.IOException {
        new AppContext(other, clock);
        assertEquals(java.nio.file.Files.readAllLines(dir.resolve("trips.txt")),
                java.nio.file.Files.readAllLines(other.resolve("trips.txt")));
    }
}
