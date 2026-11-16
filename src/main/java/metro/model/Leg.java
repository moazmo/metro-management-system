package metro.model;

/**
 * One stretch of a journey that is travelled on a single line, for example
 * "Line 1 from Helwan to Sadat, 18 stops".
 */
public class Leg {

    private final Line line;
    private final Station from;
    private final Station to;
    private final int stops;

    public Leg(Line line, Station from, Station to, int stops) {
        this.line = line;
        this.from = from;
        this.to = to;
        this.stops = stops;
    }

    public Line getLine() {
        return line;
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public int getStops() {
        return stops;
    }
}
