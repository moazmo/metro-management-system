package metro.model;

/**
 * The service status of a trip (specification 2c), and which changes of
 * status are allowed.
 *
 *   Scheduled -> Delayed, Completed, Cancelled
 *   Delayed   -> Delayed (a new delay), Completed, Cancelled
 *   Completed and Cancelled are final.
 */
public enum TripStatus {
    SCHEDULED("Scheduled"),
    DELAYED("Delayed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TripStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** A final status can never change again. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** Rule: whether a trip with this status may move to the next one. */
    public boolean canChangeTo(TripStatus next) {
        // A trip that is not finished may become Delayed, Completed or Cancelled, never Scheduled again.
        return !isFinal() && next != SCHEDULED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
