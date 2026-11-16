package metro.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import metro.model.FareTier;

/**
 * The fare depends on how far the passenger travels, in steps (tiers):
 * for example up to 9 stops costs 8, up to 16 costs 10, and so on.
 */
public class TieredFarePolicy implements FarePolicy {

    private final List<FareTier> tiers;

    /** Puts tiers in order of the number of stops they cover. */
    static class ByMaxStops implements Comparator<FareTier> {
        @Override
        public int compare(FareTier a, FareTier b) {
            return Integer.compare(a.getMaxStops(), b.getMaxStops());
        }
    }

    public TieredFarePolicy(List<FareTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("A fare policy needs at least one tier");
        }
        this.tiers = new ArrayList<>(tiers);
        this.tiers.sort(new ByMaxStops());
    }

    /**
     * The price of the lowest tier that covers the journey. A journey longer
     * than every tier costs the price of the highest tier.
     */
    @Override
    public double baseFare(int stops) {
        if (stops < 1) {
            throw new IllegalArgumentException("A journey has at least one stop");
        }
        for (FareTier tier : tiers) {
            if (stops <= tier.getMaxStops()) {
                return tier.getPrice();
            }
        }
        return tiers.get(tiers.size() - 1).getPrice();
    }
}
