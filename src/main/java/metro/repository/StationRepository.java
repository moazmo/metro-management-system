package metro.repository;

import java.nio.file.Path;
import metro.model.Station;
import metro.model.StationStatus;

/** Stores stations in stations.txt. */
public class StationRepository extends FileRepository<Station> {

    public StationRepository(Path file) {
        super(file, "Station", "STN", 3, "id|name|nameAr|status");
    }

    @Override
    protected Station parse(String[] fields) {
        return new Station(fields[0], fields[1], fields[2], StationStatus.valueOf(fields[3]));
    }

    @Override
    protected String[] format(Station station) {
        return new String[] {
            station.getId(), station.getName(), station.getNameAr(), station.getStatus().name()
        };
    }

    /** Finds a station by name, ignoring capital letters and surrounding spaces. */
    public Station findByName(String name) {
        if (name == null) {
            return null;
        }
        for (Station station : findAll()) {
            if (station.getName().equalsIgnoreCase(name.trim())) {
                return station;
            }
        }
        return null;
    }
}
