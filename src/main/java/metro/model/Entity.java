package metro.model;

/**
 * Base class of every record the system stores (stations, trains, users...).
 *
 * It holds the id and nothing else. The id is final and there is no setter,
 * so an id can never change after the object is created. Two entities are
 * equal when they have the same id.
 */
public abstract class Entity implements Identifiable {

    private final String id;

    protected Entity(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("An entity needs an id");
        }
        this.id = id;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Entity)) {
            return false;
        }
        return id.equals(((Entity) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
