package metro.model;

import metro.util.Check;

/**
 * A metro station. It has an English name, an Arabic name and a status.
 * Which lines a station is on is not stored here; a Line holds its stations.
 */
public class Station extends Entity {

    private String name;
    private String nameAr;
    private StationStatus status;

    public Station(String id, String name, String nameAr, StationStatus status) {
        super(id);
        setName(name);
        setNameAr(nameAr);
        setStatus(status);
    }

    public String getName() {
        return name;
    }

    /** Rule: a station must have a name. */
    public void setName(String name) {
        this.name = Check.notBlank(name, "Station name");
    }

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr == null ? "" : nameAr.trim();
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("A station needs a status");
        }
        this.status = status;
    }

    public boolean isOpen() {
        return status == StationStatus.OPEN;
    }

    @Override
    public String toString() {
        return name;
    }
}
