package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Line;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class LineServiceTest {

    @TempDir
    Path dir;
    TestData data;
    LineService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.lineService;
        data.loginAs(data.admin);
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    @Test
    void needsTwoStations() {
        assertRefused("A line needs at least two stations",
                () -> service.create("Short", "#000000", List.of(data.stationA)));
    }

    @Test
    void noStationTwice() {
        assertRefused("A station can appear only once on a line",
                () -> service.create("Loop", "#000000", List.of(data.stationA, data.stationB, data.stationA)));
    }

    @Test
    void badColour() {
        assertRefused("Colour must look like #1A2B3C",
                () -> service.create("L", "red", List.of(data.stationA, data.stationB)));
    }

    @Test
    void duplicateName() {
        assertRefused("A line named red already exists",
                () -> service.create("red", "#000000", List.of(data.stationA, data.stationB)));
    }

    @Test
    void createIsSaved() {
        Line line = service.create("Purple", "#7b1fa2", List.of(data.stationX, data.stationA));
        assertEquals("#7B1FA2", line.getColorHex());
        assertEquals("LIN-05|Purple|#7B1FA2|STN-010,STN-001", data.lineStarting("lines.txt", "LIN-05|"));
    }

    @Test
    void reorderIsSaved() {
        service.update(data.red.getId(), "Red", "#D32F2F",
                List.of(data.stationE, data.stationD, data.stationC, data.stationB, data.stationA));
        assertSame(data.stationE, data.red.getFirstStation());
        assertEquals("E", data.reloadedLines().findByName("Red").getFirstStation().getName());
    }

    @Test
    void refusedUpdateChangesNothing() {
        assertRefused("A line needs at least two stations",
                () -> service.update(data.red.getId(), "Crimson", "#D32F2F", List.of(data.stationA)));
        assertEquals("Red", data.red.getName());
        assertEquals(5, data.red.size());
    }

    @Test
    void controllerCannotEditLines() {
        data.loginAs(data.controller);
        assertThrows(AccessDeniedException.class,
                () -> service.update(data.red.getId(), "Red", "#D32F2F", data.red.getStations()));
        assertEquals(4, service.listAll().size());
    }

    @Test
    void deleteRemovesTheLine() {
        service.delete(data.island.getId());
        assertNull(data.lineStarting("lines.txt", data.island.getId() + "|"));
    }

    @Test
    void searchMatchesLineOrStationName() {
        assertEquals(1, service.search("gre").size());
        assertEquals(2, service.search("c").size());
        assertEquals(4, service.search(" ").size());
    }

    @Test
    void lineWithTripsCannotBeDeleted() {
        data.tripService.create(data.train1, data.red, metro.model.Direction.FORWARD,
                TestData.at(12, 0), TestData.at(12, 45));
        assertRefused("Red has trips and cannot be deleted.", () -> service.delete(data.red.getId()));
    }
}
