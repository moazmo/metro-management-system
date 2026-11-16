package metro.model;

/**
 * Where a ticket is in its life. "Expired" is not stored: a ticket that is
 * still Valid but was sold on an earlier day is expired (see Ticket.isExpired).
 */
public enum TicketStatus {
    VALID("Valid"),
    USED("Used"),
    REFUNDED("Refunded");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
