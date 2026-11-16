package metro.model;

import java.time.LocalDateTime;

/** A student ticket: half price. */
public class StudentTicket extends Ticket {

    public StudentTicket(String id, Station from, Station to, int stops, double baseFare,
                         String soldById, LocalDateTime soldAt) {
        super(id, from, to, stops, baseFare, soldById, soldAt);
    }

    @Override
    public TicketType getType() {
        return TicketType.STUDENT;
    }

    @Override
    public double discountRate() {
        return 0.50;
    }
}
