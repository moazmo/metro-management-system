package metro.service;

import java.util.ArrayList;
import java.util.List;
import metro.exception.ValidationException;
import metro.model.Line;
import metro.model.Permission;
import metro.model.Station;
import metro.model.StationStatus;
import metro.repository.AuditLog;
import metro.repository.LineRepository;
import metro.repository.StationRepository;
import metro.util.Check;

/**
 * The rules for stations (specification 1a): add, update, delete, list, search.
 */
public class StationService {

    private final StationRepository stations;
    private final LineRepository lines;
    private final Session session;
    private final AuditLog audit;

    public StationService(StationRepository stations, LineRepository lines, Session session, AuditLog audit) {
        this.stations = stations;
        this.lines = lines;
        this.session = session;
        this.audit = audit;
    }

    public List<Station> listAll() {
        session.require(Permission.VIEW_NETWORK);
        return stations.findAll();
    }

    /** Stations whose name, Arabic name or id contains the text. Blank text gives all of them. */
    public List<Station> search(String text) {
        session.require(Permission.VIEW_NETWORK);
        String wanted = text == null ? "" : text.trim().toLowerCase();
        List<Station> found = new ArrayList<>();
        for (Station station : stations.findAll()) {
            if (wanted.isEmpty()
                    || station.getName().toLowerCase().contains(wanted)
                    || station.getNameAr().contains(wanted)
                    || station.getId().toLowerCase().contains(wanted)) {
                found.add(station);
            }
        }
        return found;
    }

    /** Adds a station. New stations are open. */
    public Station create(String name, String nameAr) {
        session.require(Permission.MANAGE_STATIONS);
        String cleanName = Check.notBlank(name, "Station name");
        checkNameIsFree(cleanName, null);
        Station station = new Station(stations.nextId(), cleanName, nameAr, StationStatus.OPEN);
        stations.save(station);
        record("STATION_CREATED", station);
        return station;
    }

    public Station update(String id, String name, String nameAr, StationStatus status) {
        session.require(Permission.MANAGE_STATIONS);
        Station station = stations.require(id);
        String cleanName = Check.notBlank(name, "Station name");
        checkNameIsFree(cleanName, station);
        new Station(id, cleanName, nameAr, status); // checks every value before the real station is touched
        station.setName(cleanName);
        station.setNameAr(nameAr);
        station.setStatus(status);
        stations.save(station);
        record("STATION_UPDATED", station);
        return station;
    }

    /**
     * Deletes a station.
     * Rule: a station that is on a line cannot be deleted; it must be taken
     * off the line first, or simply marked Closed.
     */
    public void delete(String id) {
        session.require(Permission.MANAGE_STATIONS);
        Station station = stations.require(id);
        List<Line> onLines = lines.findByStation(station);
        if (!onLines.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (Line line : onLines) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(line.getName());
            }
            throw new ValidationException(station.getName() + " is on " + names
                    + ". Remove it from the line first, or mark it Closed.");
        }
        stations.delete(id);
        record("STATION_DELETED", station);
    }

    public List<Line> linesOf(Station station) {
        session.require(Permission.VIEW_NETWORK);
        return lines.findByStation(station);
    }

    /** A station on two or more lines is an interchange. */
    public boolean isInterchange(Station station) {
        session.require(Permission.VIEW_NETWORK);
        return lines.findByStation(station).size() >= 2;
    }

    /** Rule: two stations cannot have the same name (capital letters and spaces are ignored). */
    private void checkNameIsFree(String name, Station allowed) {
        Station other = stations.findByName(name);
        if (other != null && !other.equals(allowed)) {
            throw new ValidationException("A station named " + name + " already exists");
        }
    }

    private void record(String action, Station station) {
        audit.record(session.getUser().getUsername(), action, station.getName());
    }
}
