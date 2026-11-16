package metro.service;

import java.util.List;
import metro.exception.ValidationException;
import metro.model.FareTier;
import metro.model.Permission;
import metro.repository.AuditLog;
import metro.repository.FareTierRepository;

/**
 * The fare table: every logged-in user can read it, only admins can change it.
 */
public class FareService {

    private final FareTierRepository tiers;
    private final Session session;
    private final AuditLog audit;

    public FareService(FareTierRepository tiers, Session session, AuditLog audit) {
        this.tiers = tiers;
        this.session = session;
        this.audit = audit;
    }

    /** The tiers, the shortest journeys first. */
    public List<FareTier> listTiers() {
        session.requireLogin();
        List<FareTier> all = tiers.findAll();
        all.sort(new TieredFarePolicy.ByMaxStops());
        return all;
    }

    /** The fare policy built from the tiers as they are right now. */
    public FarePolicy currentPolicy() {
        session.requireLogin();
        return new TieredFarePolicy(tiers.findAll());
    }

    public FareTier addTier(int maxStops, double price) {
        session.require(Permission.MANAGE_FARES);
        checkStopsAreFree(maxStops, null);
        new FareTier("TRIAL", maxStops, price); // checks the values before an id is used up
        FareTier tier = new FareTier(tiers.nextId(), maxStops, price);
        tiers.save(tier);
        record("FARE_TIER_ADDED", tier);
        return tier;
    }

    public FareTier updateTier(String id, int maxStops, double price) {
        session.require(Permission.MANAGE_FARES);
        FareTier tier = tiers.require(id);
        checkStopsAreFree(maxStops, tier);
        new FareTier(id, maxStops, price); // checks the values before the real tier is touched
        tier.setMaxStops(maxStops);
        tier.setPrice(price);
        tiers.save(tier);
        record("FARE_TIER_UPDATED", tier);
        return tier;
    }

    /** Rule: there must always be at least one tier, or no fare could be worked out. */
    public void deleteTier(String id) {
        session.require(Permission.MANAGE_FARES);
        FareTier tier = tiers.require(id);
        if (tiers.findAll().size() == 1) {
            throw new ValidationException("At least one fare tier is required");
        }
        tiers.delete(id);
        record("FARE_TIER_DELETED", tier);
    }

    /** Rule: two tiers cannot cover the same number of stops. */
    private void checkStopsAreFree(int maxStops, FareTier allowed) {
        for (FareTier other : tiers.findAll()) {
            if (other.getMaxStops() == maxStops && !other.equals(allowed)) {
                throw new ValidationException("A tier for up to " + maxStops + " stops already exists");
            }
        }
    }

    private void record(String action, FareTier tier) {
        audit.record(session.getUser().getUsername(), action,
                "up to " + tier.getMaxStops() + " stops: " + tier.getPrice());
    }
}
