package metro.exception;

/** The logged-in user does not have the permission the operation needs. */
public class AccessDeniedException extends MetroException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
