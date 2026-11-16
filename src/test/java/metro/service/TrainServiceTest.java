package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Direction;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.model.Trip;
import metro.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

class TrainServiceTest {

    @TempDir
    Path dir;
    TestData data;
    TrainService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.trainService;
        data.loginAs(data.admin);
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    @Test
    void survivesReload() {
        TrainRepository fresh = new TrainRepository(dir.resolve("trains.txt"));
        fresh.load();
        assertEquals(3, fresh.findAll().size());
        Train third = fresh.findByCode("t-103");
        assertEquals(1000, third.getCapacity());
        assertEquals(TrainStatus.MAINTENANCE, third.getStatus());
    }

    @Test
    void fileLineLooksRight() {
        assertEquals("TRN-001|T-101|1200|ACTIVE", data.lineStarting("trains.txt", "TRN-001|"));
    }

    @Test
    void codeIsStoredUpperCase() {
        assertEquals("T-200", service.create(" t-200 ", 800).getCode());
    }

    @Test
    void duplicateCode() {
        assertRefused("A train with ID T-101 already exists", () -> service.create("t-101", 800));
    }

    @Test
    void codeCanBeChanged() {
        String id = data.train1.getId();
        service.update(id, "T-901", 1200, TrainStatus.ACTIVE);
        assertEquals(id, data.train1.getId());
        assertSame(data.train1, data.trains.findByCode("T-901"));
        assertEquals("TRN-001|T-901|1200|ACTIVE", data.lineStarting("trains.txt", "TRN-001|"));
    }

    @Test
    void updateMayKeepOwnCode() {
        service.update(data.train1.getId(), "T-101", 1500, TrainStatus.ACTIVE);
        assertEquals(1500, data.train1.getCapacity());
    }

    @Test
    void capacityRange() {
        assertRefused("Capacity must be between 1 and 3000", () -> service.create("T-300", 0));
        assertRefused("Capacity must be between 1 and 3000", () -> service.create("T-300", 3001));
    }

    @Test
    void refusedUpdateChangesNothing() {
        assertRefused("Capacity must be between 1 and 3000",
                () -> service.update(data.train1.getId(), "T-555", 0, TrainStatus.RETIRED));
        assertEquals("T-101", data.train1.getCode());
        assertEquals(TrainStatus.ACTIVE, data.train1.getStatus());
    }

    @Test
    void listActiveLeavesOutMaintenance() {
        assertFalse(service.listActive().contains(data.train3));
        assertEquals(2, service.listActive().size());
    }

    @Test
    void controllerCanListButNotCreate() {
        data.loginAs(data.controller);
        assertEquals(3, service.listAll().size());
        assertThrows(AccessDeniedException.class, () -> service.create("T-300", 800));
    }

    @Test
    void deleteRemovesTheTrain() {
        service.delete(data.train2.getId());
        assertNull(data.lineStarting("trains.txt", "TRN-002|"));
    }

    @Test
    void searchMatchesCodeIdAndStatus() {
        assertEquals(1, service.search("t-102").size());
        assertEquals(1, service.search("maintenance").size());
        assertTrue(service.search("trn-00").size() == 3);
    }

    // ----- rules that depend on trips (Task 13) -----

    private Trip baseTrip() {
        return data.tripService.create(data.train1, data.red, Direction.FORWARD,
                TestData.at(12, 0), TestData.at(12, 45));
    }

    @Test
    void trainWithUpcomingTripsCannotGoToMaintenance() {
        baseTrip();
        assertRefused("Train T-101 still has upcoming trips (1). Cancel or reassign them first.",
                () -> service.update(data.train1.getId(), "T-101", 1200, TrainStatus.MAINTENANCE));
        assertEquals(TrainStatus.ACTIVE, data.train1.getStatus());
    }

    @Test
    void trainWithHistoryCannotBeDeleted() {
        baseTrip();
        assertRefused("Train T-101 has trip history and cannot be deleted. Mark it Retired instead.",
                () -> service.delete(data.train1.getId()));
    }

    @Test
    void trainWithOnlyFinishedTripsCanRetire() {
        Trip trip = baseTrip();
        data.tripService.complete(trip.getId());
        data.clock.set(TestData.on(16, 10, 0));
        service.update(data.train1.getId(), "T-101", 1200, TrainStatus.RETIRED);
        assertEquals(TrainStatus.RETIRED, data.train1.getStatus());
    }
}
