package metro.model;

import java.time.LocalDateTime;

/** A full-price ticket. */
public class StandardTicket extends Ticket {

    public StandardTicket(String id, Station from, Station to, int stops, double baseFare,
                          String soldById, LocalDateTime soldAt) {
        super(id, from, to, stops, baseFare, soldById, soldAt);
    }

    @Override
    public TicketType getType() {
        return TicketType.STANDARD;
    }

    @Override
    public double discountRate() {
        return 0.0;
    }
}
