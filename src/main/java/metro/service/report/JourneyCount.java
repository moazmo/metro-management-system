package metro.service.report;

import metro.model.Station;
import metro.util.Money;

/** One row of the ticket report: how many tickets were sold from one station to another. */
public class JourneyCount {

    private final Station from;
    private final Station to;
    private int tickets;
    private double revenue;

    JourneyCount(Station from, Station to) {
        this.from = from;
        this.to = to;
    }

    /** Counts one more ticket for this journey. Only the ticket report calls this. */
    void add(double fare) {
        tickets++;
        revenue = Money.round(revenue + fare);
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public int getTickets() {
        return tickets;
    }

    public double getRevenue() {
        return revenue;
    }
}
