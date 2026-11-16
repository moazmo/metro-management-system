package metro.exception;

/** The input broke a rule (a blank name, a duplicate, a ticket already used...). */
public class ValidationException extends MetroException {

    public ValidationException(String message) {
        super(message);
    }
}
