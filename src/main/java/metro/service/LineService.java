package metro.service;

import java.util.ArrayList;
import java.util.List;
import metro.exception.ValidationException;
import metro.model.Line;
import metro.model.Permission;
import metro.model.Station;
import metro.repository.AuditLog;
import metro.repository.LineRepository;
import metro.util.Check;

/**
 * The rules for lines and their station sequences (specification 1b).
 */
public class LineService {

    private final LineRepository lines;
    private final Session session;
    private final AuditLog audit;

    public LineService(LineRepository lines, Session session, AuditLog audit) {
        this.lines = lines;
        this.session = session;
        this.audit = audit;
    }

    public List<Line> listAll() {
        session.require(Permission.VIEW_NETWORK);
        return lines.findAll();
    }

    /** Lines whose name, or one of whose station names, contains the text. */
    public List<Line> search(String text) {
        session.require(Permission.VIEW_NETWORK);
        String wanted = text == null ? "" : text.trim().toLowerCase();
        List<Line> found = new ArrayList<>();
        for (Line line : lines.findAll()) {
            boolean matches = wanted.isEmpty() || line.getName().toLowerCase().contains(wanted);
            for (Station station : line.getStations()) {
                if (station.getName().toLowerCase().contains(wanted)) {
                    matches = true;
                }
            }
            if (matches) {
                found.add(line);
            }
        }
        return found;
    }

    public Line create(String name, String colorHex, List<Station> stations) {
        session.require(Permission.MANAGE_LINES);
        String cleanName = check(name, stations, null);
        Line line = new Line(lines.nextId(), cleanName, colorHex, stations);
        lines.save(line);
        record("LINE_CREATED", line);
        return line;
    }

    /** Changes a line's name, colour and station sequence in one step. */
    public Line update(String id, String name, String colorHex, List<Station> stations) {
        session.require(Permission.MANAGE_LINES);
        Line line = lines.require(id);
        String cleanName = check(name, stations, line);
        new Line(id, cleanName, colorHex, stations); // checks every value before the real line is touched
        line.setName(cleanName);
        line.setColorHex(colorHex);
        line.setStations(stations);
        lines.save(line);
        record("LINE_UPDATED", line);
        return line;
    }

    public void delete(String id) {
        session.require(Permission.MANAGE_LINES);
        Line line = lines.require(id);
        lines.delete(id);
        record("LINE_DELETED", line);
    }

    /**
     * Rule: line names are unique (capital letters and spaces are ignored).
     * Rule: a line needs at least two stations.
     */
    private String check(String name, List<Station> stations, Line allowed) {
        String cleanName = Check.notBlank(name, "Line name");
        Line other = lines.findByName(cleanName);
        if (other != null && !other.equals(allowed)) {
            throw new ValidationException("A line named " + cleanName + " already exists");
        }
        if (stations == null || stations.size() < 2) {
            throw new ValidationException("A line needs at least two stations");
        }
        return cleanName;
    }

    private void record(String action, Line line) {
        audit.record(session.getUser().getUsername(), action, line.getName());
    }
}
