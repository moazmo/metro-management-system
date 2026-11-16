package metro.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import metro.exception.ValidationException;
import metro.util.Check;

/**
 * A metro line: a name, a colour and an ordered list of stations.
 *
 * The line does not own its stations. A station exists on its own and can be
 * on several lines at once (an interchange); the line only keeps references
 * to them, in travel order. This relationship is called aggregation.
 */
public class Line extends Entity {

    private String name;
    private String colorHex;
    private List<Station> stations = new ArrayList<>();

    public Line(String id, String name, String colorHex, List<Station> stations) {
        super(id);
        setName(name);
        setColorHex(colorHex);
        setStations(stations);
    }

    public String getName() {
        return name;
    }

    /** Rule: a line must have a name. */
    public void setName(String name) {
        this.name = Check.notBlank(name, "Line name");
    }

    public String getColorHex() {
        return colorHex;
    }

    /** Rule: the colour is written like #1A2B3C. It is stored in capital letters. */
    public void setColorHex(String colorHex) {
        if (colorHex == null || !colorHex.trim().matches("#[0-9A-Fa-f]{6}")) {
            throw new ValidationException("Colour must look like #1A2B3C");
        }
        this.colorHex = colorHex.trim().toUpperCase();
    }

    /** The stations in travel order. The list cannot be changed from outside. */
    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    /** Rule: a station can appear only once on a line. */
    public void setStations(List<Station> stations) {
        List<Station> copy = new ArrayList<>();
        for (Station station : stations) {
            if (copy.contains(station)) {
                throw new ValidationException("A station can appear only once on a line");
            }
            copy.add(station);
        }
        this.stations = copy;
    }

    public boolean contains(Station station) {
        return stations.contains(station);
    }

    /** The position of a station on this line, starting at 0; -1 when it is not on the line. */
    public int indexOf(Station station) {
        return stations.indexOf(station);
    }

    /** True when the two stations are next to each other on this line, in either order. */
    public boolean areAdjacent(Station a, Station b) {
        int first = indexOf(a);
        int second = indexOf(b);
        return first >= 0 && second >= 0 && Math.abs(first - second) == 1;
    }

    public Station getFirstStation() {
        return stations.get(0);
    }

    public Station getLastStation() {
        return stations.get(stations.size() - 1);
    }

    public int size() {
        return stations.size();
    }

    @Override
    public String toString() {
        return name;
    }
}
