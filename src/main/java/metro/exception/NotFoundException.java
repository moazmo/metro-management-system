package metro.exception;

/** A record with the requested id does not exist. */
public class NotFoundException extends MetroException {

    public NotFoundException(String message) {
        super(message);
    }
}
