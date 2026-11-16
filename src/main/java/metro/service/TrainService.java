package metro.service;

import java.util.ArrayList;
import java.util.List;
import metro.exception.ValidationException;
import metro.model.Permission;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.repository.AuditLog;
import metro.repository.TrainRepository;
import metro.repository.TripRepository;
import metro.util.Check;

/**
 * The rules for trains (specification 1c): train IDs, capacities and service status.
 */
public class TrainService {

    private final TrainRepository trains;
    private final TripRepository trips;
    private final Session session;
    private final AuditLog audit;

    public TrainService(TrainRepository trains, TripRepository trips, Session session, AuditLog audit) {
        this.trains = trains;
        this.trips = trips;
        this.session = session;
        this.audit = audit;
    }

    public List<Train> listAll() {
        session.require(Permission.VIEW_TRIPS);
        return trains.findAll();
    }

    /** Only the trains that may be scheduled. */
    public List<Train> listActive() {
        session.require(Permission.VIEW_TRIPS);
        List<Train> active = new ArrayList<>();
        for (Train train : trains.findAll()) {
            if (train.isActive()) {
                active.add(train);
            }
        }
        return active;
    }

    /** Trains whose train ID, id or status contains the text. */
    public List<Train> search(String text) {
        session.require(Permission.VIEW_TRIPS);
        String wanted = text == null ? "" : text.trim().toLowerCase();
        List<Train> found = new ArrayList<>();
        for (Train train : trains.findAll()) {
            if (wanted.isEmpty()
                    || train.getCode().toLowerCase().contains(wanted)
                    || train.getId().toLowerCase().contains(wanted)
                    || train.getStatus().getDisplayName().toLowerCase().contains(wanted)) {
                found.add(train);
            }
        }
        return found;
    }

    /** Adds a train. New trains are active. */
    public Train create(String code, int capacity) {
        session.require(Permission.MANAGE_TRAINS);
        String cleanCode = Check.notBlank(code, "Train ID").toUpperCase();
        checkCodeIsFree(cleanCode, null);
        Train train = new Train(trains.nextId(), cleanCode, capacity, TrainStatus.ACTIVE);
        trains.save(train);
        record("TRAIN_CREATED", train);
        return train;
    }

    public Train update(String id, String code, int capacity, TrainStatus status) {
        session.require(Permission.MANAGE_TRAINS);
        Train train = trains.require(id);
        String cleanCode = Check.notBlank(code, "Train ID").toUpperCase();
        checkCodeIsFree(cleanCode, train);
        new Train(id, cleanCode, capacity, status); // checks every value before the real train is touched
        // Rule: a train cannot leave service while it still has trips to run.
        int unfinished = trips.countUnfinished(train);
        if (train.isActive() && status != TrainStatus.ACTIVE && unfinished > 0) {
            throw new ValidationException("Train " + train.getCode() + " still has upcoming trips ("
                    + unfinished + "). Cancel or reassign them first.");
        }
        train.setCode(cleanCode);
        train.setCapacity(capacity);
        train.setStatus(status);
        trains.save(train);
        record("TRAIN_UPDATED", train);
        return train;
    }

    public void delete(String id) {
        session.require(Permission.MANAGE_TRAINS);
        Train train = trains.require(id);
        // Rule: a train that has ever run a trip is kept, so the trip history stays complete.
        if (!trips.findByTrain(train).isEmpty()) {
            throw new ValidationException("Train " + train.getCode()
                    + " has trip history and cannot be deleted. Mark it Retired instead.");
        }
        trains.delete(id);
        record("TRAIN_DELETED", train);
    }

    /** Rule: two trains cannot have the same train ID. */
    private void checkCodeIsFree(String code, Train allowed) {
        Train other = trains.findByCode(code);
        if (other != null && !other.equals(allowed)) {
            throw new ValidationException("A train with ID " + code + " already exists");
        }
    }

    private void record(String action, Train train) {
        audit.record(session.getUser().getUsername(), action, train.getCode());
    }
}
