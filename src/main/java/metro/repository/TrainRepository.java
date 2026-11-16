package metro.repository;

import java.nio.file.Path;
import metro.model.Train;
import metro.model.TrainStatus;

/** Stores trains in trains.txt. */
public class TrainRepository extends FileRepository<Train> {

    public TrainRepository(Path file) {
        super(file, "Train", "TRN", 3, "id|code|capacity|status");
    }

    @Override
    protected Train parse(String[] fields) {
        return new Train(fields[0], fields[1], Integer.parseInt(fields[2]), TrainStatus.valueOf(fields[3]));
    }

    @Override
    protected String[] format(Train train) {
        return new String[] {
            train.getId(), train.getCode(), String.valueOf(train.getCapacity()), train.getStatus().name()
        };
    }

    /** Finds a train by its train ID (code), ignoring capital letters and surrounding spaces. */
    public Train findByCode(String code) {
        if (code == null) {
            return null;
        }
        for (Train train : findAll()) {
            if (train.getCode().equalsIgnoreCase(code.trim())) {
                return train;
            }
        }
        return null;
    }
}
