package metro.repository;

import java.util.List;
import metro.model.Identifiable;

/**
 * What every store of records can do, whatever the record type is.
 *
 * The rest of the program talks to this interface, so it does not need to
 * know that the records live in text files.
 */
public interface Repository<T extends Identifiable> {

    /** All records, as a new list, in the order they are stored. */
    List<T> findAll();

    /** The record with this id, or null when there is none. */
    T findById(String id);

    /** The record with this id; throws NotFoundException when there is none. */
    T require(String id);

    /** Adds the record when its id is new, otherwise replaces the stored one. */
    void save(T item);

    /** Removes the record with this id; throws NotFoundException when there is none. */
    void delete(String id);

    /** A new id that has never been used before, for example "STN-004". */
    String nextId();
}
