package metro.exception;

/** A data file could not be read or written, or one of its lines is damaged. */
public class DataFileException extends MetroException {

    public DataFileException(String message) {
        super(message);
    }

    public DataFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
