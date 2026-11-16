package metro.repository;

import static metro.TestData.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import metro.TestData;
import metro.exception.DataFileException;
import metro.model.Direction;
import metro.model.Trip;
import metro.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TripRepositoryTest {

    @TempDir
    Path dir;
    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
    }

    private TripRepository reload() {
        TripRepository fresh = new TripRepository(dir.resolve("trips.txt"), data.trains, data.lines);
        fresh.load();
        return fresh;
    }

    private Trip add(Direction direction, int hour) {
        Trip trip = new Trip(data.trips.nextId(), data.train1, data.red, direction, at(hour, 0), at(hour, 45));
        data.trips.save(trip);
        return trip;
    }

    @Test
    void survivesReloadWithStatusAndDelay() {
        Trip trip = add(Direction.BACKWARD, 10);
        trip.changeStatus(TripStatus.DELAYED);
        trip.setDelayMinutes(15);
        data.trips.save(trip);

        Trip loaded = reload().require(trip.getId());
        assertSame(data.train1, loaded.getTrain());
        assertSame(data.red, loaded.getLine());
        assertEquals(Direction.BACKWARD, loaded.getDirection());
        assertEquals(at(10, 0), loaded.getDeparture());
        assertEquals(at(10, 45), loaded.getArrival());
        assertEquals(TripStatus.DELAYED, loaded.getStatus());
        assertEquals(15, loaded.getDelayMinutes());
    }

    @Test
    void fileLineLooksRight() {
        add(Direction.FORWARD, 10);
        assertEquals("TRP-0001|TRN-001|LIN-01|FORWARD|2026-11-15T10:00:00|2026-11-15T10:45:00|SCHEDULED|0",
                data.lineStarting("trips.txt", "TRP-0001|"));
    }

    @Test
    void unknownTrainNamesFileAndLine() throws IOException {
        Files.write(dir.resolve("trips.txt"), List.of("#next=2", "# header",
                "TRP-0001|TRN-999|LIN-01|FORWARD|2026-11-15T10:00:00|2026-11-15T10:45:00|SCHEDULED|0"),
                StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("trips.txt line 3: unknown train TRN-999"), e.getMessage());
    }

    @Test
    void unknownLineNamesFileAndLine() throws IOException {
        Files.write(dir.resolve("trips.txt"), List.of("#next=2", "# header",
                "TRP-0001|TRN-001|LIN-99|FORWARD|2026-11-15T10:00:00|2026-11-15T10:45:00|SCHEDULED|0"),
                StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("trips.txt line 3: unknown line LIN-99"), e.getMessage());
    }

    @Test
    void findsTripsByTrainAndLine() {
        add(Direction.FORWARD, 10);
        add(Direction.BACKWARD, 12);
        assertEquals(2, data.trips.findByTrain(data.train1).size());
        assertEquals(0, data.trips.findByTrain(data.train2).size());
        assertEquals(2, data.trips.findByLine(data.red).size());
        assertEquals(0, data.trips.findByLine(data.blue).size());
    }
}
