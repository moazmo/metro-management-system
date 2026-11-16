package metro.model;

/** The four kinds of staff account. Each one has its own User subclass. */
public enum Role {
    ADMIN("Admin"),
    OPERATIONS_CONTROLLER("Operations controller"),
    TICKET_AGENT("Ticket agent"),
    STATION_SUPERVISOR("Station supervisor");

    private final String displayName;

    Role(String displayName) {
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
