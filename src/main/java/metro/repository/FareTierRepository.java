package metro.repository;

import java.nio.file.Path;
import metro.model.FareTier;

/** Stores the fare table in fares.txt. */
public class FareTierRepository extends FileRepository<FareTier> {

    public FareTierRepository(Path file) {
        super(file, "Fare tier", "FAR", 2, "id|maxStops|price");
    }

    @Override
    protected FareTier parse(String[] fields) {
        return new FareTier(fields[0], Integer.parseInt(fields[1]), Double.parseDouble(fields[2]));
    }

    @Override
    protected String[] format(FareTier tier) {
        return new String[] {tier.getId(), String.valueOf(tier.getMaxStops()), String.valueOf(tier.getPrice())};
    }
}
