package metro.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import metro.model.Line;
import metro.model.Station;

/**
 * Stores lines in lines.txt. A line is saved with the ids of its stations,
 * in order; when the file is read, each id is turned back into the Station
 * object held by the StationRepository. That is why stations must be loaded
 * before lines.
 */
public class LineRepository extends FileRepository<Line> {

    private final StationRepository stations;

    public LineRepository(Path file, StationRepository stations) {
        super(file, "Line", "LIN", 2, "id|name|colour|stationIds");
        this.stations = stations;
    }

    @Override
    protected Line parse(String[] fields) {
        List<Station> onLine = new ArrayList<>();
        for (String stationId : fields[3].split(",")) {
            if (stationId.isBlank()) {
                continue;
            }
            Station station = stations.findById(stationId.trim());
            if (station == null) {
                throw new IllegalArgumentException("unknown station " + stationId.trim());
            }
            onLine.add(station);
        }
        return new Line(fields[0], fields[1], fields[2], onLine);
    }

    @Override
    protected String[] format(Line line) {
        StringBuilder ids = new StringBuilder();
        for (Station station : line.getStations()) {
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append(station.getId());
        }
        return new String[] {line.getId(), line.getName(), line.getColorHex(), ids.toString()};
    }

    /** Finds a line by name, ignoring capital letters and surrounding spaces. */
    public Line findByName(String name) {
        if (name == null) {
            return null;
        }
        for (Line line : findAll()) {
            if (line.getName().equalsIgnoreCase(name.trim())) {
                return line;
            }
        }
        return null;
    }

    /** Every line the station is on. */
    public List<Line> findByStation(Station station) {
        List<Line> found = new ArrayList<>();
        for (Line line : findAll()) {
            if (line.contains(station)) {
                found.add(line);
            }
        }
        return found;
    }
}
