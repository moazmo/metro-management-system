package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Station;
import metro.model.StationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StationServiceTest {

    @TempDir
    Path dir;
    TestData data;
    StationService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.stationService;
        data.loginAs(data.admin);
    }

    @Test
    void agentCannotCreate() {
        data.loginAs(data.agent);
        assertThrows(AccessDeniedException.class, () -> service.create("New", ""));
        assertEquals(11, service.listAll().size());
    }

    @Test
    void duplicateNameIgnoresCaseAndSpaces() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.create("  a ", ""));
        assertEquals("A station named a already exists", e.getMessage());
    }

    @Test
    void updateMayKeepOwnName() {
        Station updated = service.update(data.stationA.getId(), "A", "ألف", StationStatus.OPEN);
        assertEquals("ألف", updated.getNameAr());
    }

    @Test
    void updateCannotTakeAnotherStationsName() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(data.stationA.getId(), "b", "", StationStatus.OPEN));
        assertEquals("A station named b already exists", e.getMessage());
        assertEquals("A", data.stationA.getName());
    }

    @Test
    void renameIsSeenThroughTheLine() {
        service.update(data.stationA.getId(), "Alpha", "", StationStatus.OPEN);
        assertEquals("Alpha", data.red.getFirstStation().getName());
        assertTrue(data.lineStarting("stations.txt", "STN-001|").startsWith("STN-001|Alpha|"));
    }

    @Test
    void closingAStationIsSaved() {
        service.update(data.stationB.getId(), "B", "", StationStatus.CLOSED);
        assertFalse(data.stationB.isOpen());
        assertTrue(data.lineStarting("stations.txt", "STN-002|").endsWith("|CLOSED"));
    }

    @Test
    void stationOnALineCannotBeDeleted() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.delete(data.stationC.getId()));
        assertEquals("C is on Red, Blue. Remove it from the line first, or mark it Closed.", e.getMessage());
    }

    @Test
    void freeStationCanBeDeleted() {
        Station z = service.create("Z", "");
        service.delete(z.getId());
        assertNull(data.lineStarting("stations.txt", z.getId() + "|"));
    }

    @Test
    void interchangeIsDetected() {
        assertTrue(service.isInterchange(data.stationC));
        assertFalse(service.isInterchange(data.stationA));
        assertEquals(2, service.linesOf(data.stationC).size());
    }

    @Test
    void searchMatchesNameArabicNameAndId() {
        service.update(data.stationA.getId(), "A", "ألف", StationStatus.OPEN);
        assertEquals(1, service.search("ألف").size());
        assertEquals(1, service.search("stn-002").size());
        assertEquals(1, service.search(" x ").size());
        assertEquals(11, service.search("").size());
    }

    @Test
    void blankNameIsRefused() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.create("   ", ""));
        assertEquals("Station name is required", e.getMessage());
    }
}
