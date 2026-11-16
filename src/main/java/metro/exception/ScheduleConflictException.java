package metro.exception;

import java.util.ArrayList;
import java.util.List;
import metro.model.Train;
import metro.model.Trip;
import metro.util.Dates;

/**
 * A trip could not be scheduled because its train is already busy at that
 * time (specification 2d). The exception carries the trips it clashes with,
 * and its message names the first one.
 */
public class ScheduleConflictException extends MetroException {

    private final List<Trip> conflicts;

    public ScheduleConflictException(Train train, List<Trip> conflicts) {
        super(describe(train, conflicts.get(0)));
        this.conflicts = new ArrayList<>(conflicts);
    }

    private static String describe(Train train, Trip clash) {
        return "Train " + train.getCode() + " is already on trip " + clash.getId()
                + " (" + Dates.time(clash.getDeparture()) + "–" + Dates.time(clash.getEffectiveArrival())
                + ", " + clash.getLine().getName() + "). Pick another train or time.";
    }

    public List<Trip> getConflicts() {
        return new ArrayList<>(conflicts);
    }
}
