package metro.model;

/** The kinds of ticket. Each one has its own Ticket subclass with its own discount. */
public enum TicketType {
    STANDARD("Standard"),
    STUDENT("Student"),
    SENIOR("Senior");

    private final String displayName;

    TicketType(String displayName) {
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
