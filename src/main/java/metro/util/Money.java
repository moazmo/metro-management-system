package metro.util;

import java.util.Locale;

/**
 * Rounding and formatting of money amounts (Egyptian pounds).
 */
public final class Money {

    private Money() {
    }

    /** Rounds to two decimals; a half is rounded up. */
    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** Formats an amount for the screen, for example "EGP 1,250.50". */
    public static String format(double amount) {
        return String.format(Locale.ENGLISH, "EGP %,.2f", amount);
    }

    /** Formats an amount as a plain number with two decimals, for example "8.00". */
    public static String plain(double amount) {
        return String.format(Locale.ENGLISH, "%.2f", amount);
    }
}
