package metro.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import metro.exception.ValidationException;
import metro.model.Journey;
import metro.model.Leg;
import metro.model.Line;
import metro.model.Station;
import metro.repository.LineRepository;

/**
 * Finds the route with the fewest stops between two stations, changing
 * lines at interchange stations when needed.
 *
 * The network is treated as a graph: every station is a point, and two
 * stations are connected when they are next to each other on some line.
 * The search is a breadth-first search: it looks at all stations one stop
 * away, then all stations two stops away, and so on. The first time it
 * reaches the destination, it has therefore found a route with the fewest
 * stops. The fare of a ticket is based on that number.
 *
 * The planner has no permission check of its own; the services that use it do.
 */
public class RoutePlanner {

    private final LineRepository lines;

    public RoutePlanner(LineRepository lines) {
        this.lines = lines;
    }

    public Journey plan(Station from, Station to) {
        if (from.equals(to)) {
            throw new ValidationException("Origin and destination must be different");
        }
        // Read the lines now, so a network that was just edited is used straight away.
        List<Line> allLines = lines.findAll();

        // Breadth-first search. "cameFrom" remembers, for each station reached, the station before it.
        Map<Station, Station> cameFrom = new HashMap<>();
        Set<Station> reached = new HashSet<>();
        List<Station> toVisit = new ArrayList<>();
        toVisit.add(from);
        reached.add(from);
        int next = 0;
        while (next < toVisit.size() && !reached.contains(to)) {
            Station current = toVisit.get(next);
            next++;
            for (Station neighbour : neighbours(current, allLines)) {
                if (!reached.contains(neighbour)) {
                    reached.add(neighbour);
                    cameFrom.put(neighbour, current);
                    toVisit.add(neighbour);
                }
            }
        }
        if (!reached.contains(to)) {
            throw new ValidationException("No route between " + from.getName() + " and " + to.getName());
        }

        // Walk back from the destination to the origin to get the route.
        List<Station> route = new ArrayList<>();
        Station step = to;
        while (step != null) {
            route.add(0, step);
            step = cameFrom.get(step);
        }
        return new Journey(route, legsOf(route, allLines));
    }

    /**
     * The stations next to a station on any line. The order is always the
     * same (lines in id order, the earlier station before the later one), so
     * the same question always gives the same route.
     */
    private List<Station> neighbours(Station station, List<Line> allLines) {
        List<Station> result = new ArrayList<>();
        for (Line line : allLines) {
            int position = line.indexOf(station);
            if (position > 0) {
                result.add(line.getStations().get(position - 1));
            }
            if (position >= 0 && position < line.size() - 1) {
                result.add(line.getStations().get(position + 1));
            }
        }
        return result;
    }

    /**
     * Splits a route into legs, one per line used. The passenger stays on the
     * current line as long as it continues to the next station; otherwise a
     * new leg starts on the first line that connects the two stations.
     */
    private List<Leg> legsOf(List<Station> route, List<Line> allLines) {
        List<Leg> legs = new ArrayList<>();
        Line current = null;
        Station legStart = route.get(0);
        int legStops = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            Station a = route.get(i);
            Station b = route.get(i + 1);
            if (current == null || !current.areAdjacent(a, b)) {
                if (current != null) {
                    legs.add(new Leg(current, legStart, a, legStops));
                }
                current = lineConnecting(a, b, allLines);
                legStart = a;
                legStops = 0;
            }
            legStops++;
        }
        legs.add(new Leg(current, legStart, route.get(route.size() - 1), legStops));
        return legs;
    }

    private Line lineConnecting(Station a, Station b, List<Line> allLines) {
        for (Line line : allLines) {
            if (line.areAdjacent(a, b)) {
                return line;
            }
        }
        throw new IllegalStateException("No line connects " + a.getName() + " and " + b.getName());
    }
}
