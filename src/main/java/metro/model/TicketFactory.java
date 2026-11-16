package metro.model;

import java.time.LocalDateTime;

/**
 * Creates the right Ticket subclass for a ticket type. Used when a ticket
 * is sold and when tickets are read from the file.
 */
public final class TicketFactory {

    private TicketFactory() {
    }

    public static Ticket create(TicketType type, String id, Station from, Station to, int stops,
                                double baseFare, String soldById, LocalDateTime soldAt) {
        switch (type) {
            case STANDARD:
                return new StandardTicket(id, from, to, stops, baseFare, soldById, soldAt);
            case STUDENT:
                return new StudentTicket(id, from, to, stops, baseFare, soldById, soldAt);
            case SENIOR:
                return new SeniorTicket(id, from, to, stops, baseFare, soldById, soldAt);
            default:
                throw new IllegalArgumentException("Unknown ticket type " + type);
        }
    }
}
