package metro.model;

/** Whether a train can be used for trips. */
public enum TrainStatus {
    ACTIVE("Active"),
    MAINTENANCE("In maintenance"),
    RETIRED("Retired");

    private final String displayName;

    TrainStatus(String displayName) {
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
