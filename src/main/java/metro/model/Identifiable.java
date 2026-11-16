package metro.model;

/**
 * Anything that has an id. Repositories can store any Identifiable object
 * because the id is all they need to find, replace or delete a record.
 */
public interface Identifiable {

    String getId();
}
