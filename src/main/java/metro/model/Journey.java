package metro.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A route between two stations: every station passed, in order, and the
 * legs it is made of (one leg per line used).
 *
 * A journey is composed of its legs: the legs are created for this journey
 * and have no meaning without it. This relationship is called composition.
 */
public class Journey {

    private final List<Station> stations;
    private final List<Leg> legs;

    public Journey(List<Station> stations, List<Leg> legs) {
        this.stations = new ArrayList<>(stations);
        this.legs = new ArrayList<>(legs);
    }

    /** Every station of the route, the origin first and the destination last. */
    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    public Station getFrom() {
        return stations.get(0);
    }

    public Station getTo() {
        return stations.get(stations.size() - 1);
    }

    /** How many stations the passenger travels: one less than the stations on the route. */
    public int getStops() {
        return stations.size() - 1;
    }

    public List<Leg> getLegs() {
        return Collections.unmodifiableList(legs);
    }

    /** How many times the passenger changes line. */
    public int getTransfers() {
        return legs.size() - 1;
    }
}
