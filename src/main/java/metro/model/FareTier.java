package metro.model;

import metro.util.Check;
import metro.util.Money;

/**
 * One step of the fare table: "a journey of up to maxStops stops costs price".
 * The tiers are data in a file, so an admin can change fares without
 * changing the program.
 */
public class FareTier extends Entity {

    private int maxStops;
    private double price;

    public FareTier(String id, int maxStops, double price) {
        super(id);
        setMaxStops(maxStops);
        setPrice(price);
    }

    public int getMaxStops() {
        return maxStops;
    }

    /** Rule: a tier covers from 1 to 200 stops. */
    public void setMaxStops(int maxStops) {
        this.maxStops = Check.range(maxStops, 1, 200, "Stops");
    }

    public double getPrice() {
        return price;
    }

    /** Rule: a price is greater than zero. It is kept with two decimals. */
    public void setPrice(double price) {
        this.price = Money.round(Check.positive(price, "Price"));
    }
}
