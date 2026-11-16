package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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
}
