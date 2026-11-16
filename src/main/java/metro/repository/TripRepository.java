package metro.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import metro.model.Direction;
import metro.model.Line;
import metro.model.Train;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.util.Dates;

/**
 * Stores trips in trips.txt. A trip is saved with the ids of its train and
 * its line, so trains and lines must be loaded before trips.
 */
public class TripRepository extends FileRepository<Trip> {

    private final TrainRepository trains;
    private final LineRepository lines;

    public TripRepository(Path file, TrainRepository trains, LineRepository lines) {
        super(file, "Trip", "TRP", 4, "id|trainId|lineId|direction|departure|arrival|status|delayMinutes");
        this.trains = trains;
        this.lines = lines;
    }

    @Override
    protected Trip parse(String[] fields) {
        Train train = trains.findById(fields[1]);
        if (train == null) {
            throw new IllegalArgumentException("unknown train " + fields[1]);
        }
        Line line = lines.findById(fields[2]);
        if (line == null) {
            throw new IllegalArgumentException("unknown line " + fields[2]);
        }
        return new Trip(fields[0], train, line, Direction.valueOf(fields[3]),
                Dates.parse(fields[4]), Dates.parse(fields[5]),
                TripStatus.valueOf(fields[6]), Integer.parseInt(fields[7]));
    }

    @Override
    protected String[] format(Trip trip) {
        return new String[] {
            trip.getId(), trip.getTrain().getId(), trip.getLine().getId(), trip.getDirection().name(),
            Dates.file(trip.getDeparture()), Dates.file(trip.getArrival()),
            trip.getStatus().name(), String.valueOf(trip.getDelayMinutes())
        };
    }

    public List<Trip> findByTrain(Train train) {
        List<Trip> found = new ArrayList<>();
        for (Trip trip : findAll()) {
            if (trip.getTrain().equals(train)) {
                found.add(trip);
            }
        }
        return found;
    }

    public List<Trip> findByLine(Line line) {
        List<Trip> found = new ArrayList<>();
        for (Trip trip : findAll()) {
            if (trip.getLine().equals(line)) {
                found.add(trip);
            }
        }
        return found;
    }

    /** How many trips of a train are neither completed nor cancelled. */
    public int countUnfinished(Train train) {
        int count = 0;
        for (Trip trip : findByTrain(train)) {
            if (!trip.getStatus().isFinal()) {
                count++;
            }
        }
        return count;
    }
}
