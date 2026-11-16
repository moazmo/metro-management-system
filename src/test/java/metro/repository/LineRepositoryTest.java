package metro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import metro.TestData;
import metro.exception.DataFileException;
import metro.model.Line;
import metro.model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LineRepositoryTest {

    @TempDir
    Path dir;
    TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
    }

    private List<String> names(List<Station> stations) {
        List<String> names = new ArrayList<>();
        for (Station station : stations) {
            names.add(station.getName());
        }
        return names;
    }

    @Test
    void stationOrderSurvivesReload() {
        Line red = data.reloadedLines().findByName("Red");
        assertEquals(List.of("A", "B", "C", "D", "E"), names(red.getStations()));
        assertEquals("#D32F2F", red.getColorHex());
    }

    @Test
    void sharedStationIsTheSameObject() {
        LineRepository fresh = data.reloadedLines();
        Line red = fresh.findByName("Red");
        Line blue = fresh.findByName("Blue");
        assertSame(red.getStations().get(2), blue.getStations().get(1));
    }

    @Test
    void fileLineLooksRight() {
        assertEquals("LIN-01|Red|#D32F2F|STN-001,STN-002,STN-003,STN-004,STN-005",
                data.lineStarting("lines.txt", "LIN-01|"));
    }

    @Test
    void unknownStationNamesFileAndLine() throws IOException {
        Files.write(dir.resolve("lines.txt"), List.of(
                "#next=2", "# id|name|colour|stationIds", "LIN-01|Red|#D32F2F|STN-001,STN-999"),
                StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> data.reloadedLines());
        assertTrue(e.getMessage().contains("lines.txt line 3: unknown station STN-999"), e.getMessage());
    }

    @Test
    void findsLinesOfAStation() {
        assertEquals(2, data.lines.findByStation(data.stationC).size());
        assertEquals(1, data.lines.findByStation(data.stationA).size());
    }

    @Test
    void lineKnowsItsEndsAndNeighbours() {
        assertSame(data.stationA, data.red.getFirstStation());
        assertSame(data.stationE, data.red.getLastStation());
        assertEquals(5, data.red.size());
        assertTrue(data.red.areAdjacent(data.stationB, data.stationC));
        assertTrue(data.red.areAdjacent(data.stationC, data.stationB));
        assertTrue(!data.red.areAdjacent(data.stationA, data.stationC));
        assertTrue(!data.red.areAdjacent(data.stationA, data.stationF));
    }
}
