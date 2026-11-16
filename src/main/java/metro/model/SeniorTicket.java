package metro.model;

import java.time.LocalDateTime;

/** A senior ticket: 30% off. */
public class SeniorTicket extends Ticket {

    public SeniorTicket(String id, Station from, Station to, int stops, double baseFare,
                        String soldById, LocalDateTime soldAt) {
        super(id, from, to, stops, baseFare, soldById, soldAt);
    }

    @Override
    public TicketType getType() {
        return TicketType.SENIOR;
    }

    @Override
    public double discountRate() {
        return 0.30;
    }
}
