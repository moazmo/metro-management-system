package metro.service.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import metro.model.Line;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.util.Dates;

/**
 * Specification 4a: trips by date, line and service status.
 * A trip belongs to the day it departs on.
 */
public class TripReport extends Report {

    private final List<Trip> trips = new ArrayList<>();

    /** Puts trips in order of departure time. */
    private static class ByDeparture implements Comparator<Trip> {
        @Override
        public int compare(Trip a, Trip b) {
            return a.getDeparture().compareTo(b.getDeparture());
        }
    }

    /**
     * @param line   only trips on this line, or null for every line
     * @param status only trips with this status, or null for every status
     */
    public TripReport(List<Trip> allTrips, LocalDate from, LocalDate to, Line line, TripStatus status) {
        super("Trip report", from, to);
        for (Trip trip : allTrips) {
            if (inRange(trip.getDeparture())
                    && (line == null || trip.getLine().equals(line))
                    && (status == null || trip.getStatus() == status)) {
                trips.add(trip);
            }
        }
        trips.sort(new ByDeparture());
    }

    /** The matching trips, the earliest departure first. */
    public List<Trip> getTrips() {
        return new ArrayList<>(trips);
    }

    /** How many of the matching trips have the given status. */
    public int count(TripStatus status) {
        int count = 0;
        for (Trip trip : trips) {
            if (trip.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<String> getColumns() {
        return Arrays.asList("Trip", "Line", "From", "To", "Train", "Departure", "Arrival", "Status", "Delay (min)");
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (Trip trip : trips) {
            rows.add(new String[] {
                trip.getId(),
                trip.getLine().getName(),
                trip.getOrigin().getName(),
                trip.getDestination().getName(),
                trip.getTrain().getCode(),
                trip.getDeparture().format(Dates.ISO_DATE_TIME),
                trip.getEffectiveArrival().format(Dates.ISO_DATE_TIME),
                trip.getStatus().getDisplayName(),
                String.valueOf(trip.getDelayMinutes())
            });
        }
        return rows;
    }
}
