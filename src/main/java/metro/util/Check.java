package metro.util;

import metro.exception.ValidationException;

/**
 * Small validation helpers shared by the model classes and the services.
 * Each one returns the accepted value or throws a ValidationException whose
 * message can be shown to the user.
 */
public final class Check {

    private Check() {
    }

    /** Returns the text without surrounding spaces, or refuses blank text. */
    public static String notBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " is required");
        }
        return value.trim();
    }

    /** Accepts a whole number from min to max, both included. */
    public static int range(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new ValidationException(label + " must be between " + min + " and " + max);
        }
        return value;
    }

    /** Accepts a number greater than zero. */
    public static double positive(double value, String label) {
        if (!(value > 0)) {
            throw new ValidationException(label + " must be greater than zero");
        }
        return value;
    }
}
