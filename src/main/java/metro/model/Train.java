package metro.model;

import metro.util.Check;

/**
 * A train of the fleet.
 *
 * A train has two identifiers. The id (from Entity) is given by the system
 * and never changes. The code, for example "T-101", is the "train ID" the
 * staff see and may correct; it is an ordinary field with a setter.
 */
public class Train extends Entity {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 3000;

    private String code;
    private int capacity;
    private TrainStatus status;

    public Train(String id, String code, int capacity, TrainStatus status) {
        super(id);
        setCode(code);
        setCapacity(capacity);
        setStatus(status);
    }

    public String getCode() {
        return code;
    }

    /** Rule: a train must have a train ID. It is stored in capital letters. */
    public void setCode(String code) {
        this.code = Check.notBlank(code, "Train ID").toUpperCase();
    }

    public int getCapacity() {
        return capacity;
    }

    /** Rule: capacity is from 1 to 3000 passengers. */
    public void setCapacity(int capacity) {
        this.capacity = Check.range(capacity, MIN_CAPACITY, MAX_CAPACITY, "Capacity");
    }

    public TrainStatus getStatus() {
        return status;
    }

    public void setStatus(TrainStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("A train needs a status");
        }
        this.status = status;
    }

    public boolean isActive() {
        return status == TrainStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return code;
    }
}
