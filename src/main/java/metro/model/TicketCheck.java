package metro.model;

/**
 * The answer to "is this ticket acceptable?": yes or no, a message for the
 * staff member, and the ticket itself when it exists.
 */
public class TicketCheck {

    private final boolean accepted;
    private final String message;
    private final Ticket ticket;

    public TicketCheck(boolean accepted, String message, Ticket ticket) {
        this.accepted = accepted;
        this.message = message;
        this.ticket = ticket;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getMessage() {
        return message;
    }

    /** The ticket that was checked, or null when no ticket has that code. */
    public Ticket getTicket() {
        return ticket;
    }
}
