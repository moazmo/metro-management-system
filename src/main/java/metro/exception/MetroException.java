package metro.exception;

/**
 * Parent of every error this application raises on purpose.
 *
 * The message is always a full sentence that can be shown to the user as it
 * is. Screens catch MetroException and display the message; they never show
 * a stack trace.
 */
public class MetroException extends RuntimeException {

    public MetroException(String message) {
        super(message);
    }

    public MetroException(String message, Throwable cause) {
        super(message, cause);
    }
}
