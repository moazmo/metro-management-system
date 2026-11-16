package metro.model;

/** Whether passengers can use a station. */
public enum StationStatus {
    OPEN("Open"),
    CLOSED("Closed");

    private final String displayName;

    StationStatus(String displayName) {
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
