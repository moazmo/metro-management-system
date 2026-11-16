package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.FareTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

/** Demo tiers: up to 9 stops 8.00, up to 16 10.00, up to 23 15.00, up to 39 20.00. */
class FareServiceTest {

    @TempDir
    Path dir;
    TestData data;
    FareService service;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        service = data.fareService;
        data.loginAs(data.admin);
    }

    private void assertRefused(String message, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(message, e.getMessage());
    }

    // ----- the fare policy -----

    @Test
    void tierBoundaries() {
        FarePolicy policy = service.currentPolicy();
        assertEquals(8.0, policy.baseFare(1));
        assertEquals(8.0, policy.baseFare(9));
        assertEquals(10.0, policy.baseFare(10));
        assertEquals(10.0, policy.baseFare(16));
        assertEquals(15.0, policy.baseFare(17));
        assertEquals(15.0, policy.baseFare(23));
        assertEquals(20.0, policy.baseFare(24));
        assertEquals(20.0, policy.baseFare(39));
    }

    @Test
    void beyondTheLastTierUsesTheLastPrice() {
        assertEquals(20.0, service.currentPolicy().baseFare(60));
    }

    @Test
    void tiersNeedNotBeSorted() {
        List<FareTier> reversed = new ArrayList<>(service.listTiers());
        Collections.reverse(reversed);
        FarePolicy policy = new TieredFarePolicy(reversed);
        assertEquals(8.0, policy.baseFare(3));
        assertEquals(15.0, policy.baseFare(20));
        assertEquals(20.0, policy.baseFare(99));
    }

    @Test
    void zeroStopsIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> service.currentPolicy().baseFare(0));
    }

    @Test
    void aPolicyNeedsAtLeastOneTier() {
        assertThrows(IllegalArgumentException.class, () -> new TieredFarePolicy(new ArrayList<>()));
    }

    // ----- managing the tiers -----

    @Test
    void tiersAreListedLowestFirst() {
        service.addTier(5, 6);
        List<FareTier> tiers = service.listTiers();
        assertEquals(5, tiers.get(0).getMaxStops());
        assertEquals(9, tiers.get(1).getMaxStops());
        assertEquals(5, tiers.size());
    }

    @Test
    void agentCanReadButNotChange() {
        data.loginAs(data.agent);
        assertEquals(4, service.listTiers().size());
        assertEquals(8.0, service.currentPolicy().baseFare(2));
        assertThrows(AccessDeniedException.class, () -> service.addTier(5, 6));
    }

    @Test
    void nobodyLoggedInCannotRead() {
        data.session.logout();
        assertThrows(AccessDeniedException.class, () -> service.listTiers());
    }

    @Test
    void duplicateTierIsRefused() {
        assertRefused("A tier for up to 9 stops already exists", () -> service.addTier(9, 7));
    }

    @Test
    void lastTierCannotBeDeleted() {
        List<FareTier> tiers = service.listTiers();
        for (int i = 0; i < 3; i++) {
            service.deleteTier(tiers.get(i).getId());
        }
        assertRefused("At least one fare tier is required", () -> service.deleteTier(tiers.get(3).getId()));
        assertEquals(1, service.listTiers().size());
    }

    @Test
    void priceMustBePositive() {
        assertRefused("Price must be greater than zero", () -> service.addTier(5, 0));
    }

    @Test
    void stopsMustBeInRange() {
        assertRefused("Stops must be between 1 and 200", () -> service.addTier(0, 5));
        assertRefused("Stops must be between 1 and 200", () -> service.addTier(201, 5));
    }

    @Test
    void changeIsUsedImmediately() {
        FareTier first = service.listTiers().get(0);
        service.updateTier(first.getId(), 9, 9.5);
        assertEquals(9.5, service.currentPolicy().baseFare(3));
        assertEquals("FAR-01|9|9.5", data.lineStarting("fares.txt", "FAR-01|"));
    }

    @Test
    void updateMayKeepItsOwnStops() {
        FareTier first = service.listTiers().get(0);
        service.updateTier(first.getId(), 9, 8.5);
        assertRefused("A tier for up to 16 stops already exists", () -> service.updateTier(first.getId(), 16, 8.5));
        assertEquals(9, first.getMaxStops());
        assertTrue(first.getPrice() == 8.5);
    }
}
