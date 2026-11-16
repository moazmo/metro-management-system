package metro.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The date and time formats used in the data files and on screen, in one place.
 */
public final class Dates {

    /** Format of date-times inside the data files. */
    public static final DateTimeFormatter FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    /** Date as shown on screen, for example "15 Nov 2026". */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    /** Time as shown on screen, for example "14:05". */
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** Short date and time for tables, for example "15 Nov 14:05". */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH);
    /** Plain date for reports and exports, for example "2026-11-15". */
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** Plain date-time for reports and exports, for example "2026-11-15 14:05". */
    public static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Dates() {
    }

    /** Text for a data file; an absent time is written as empty text. */
    public static String file(LocalDateTime time) {
        return time == null ? "" : time.format(FILE);
    }

    /** Reads a time from a data file; empty text means there is no time. */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text, FILE);
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String time(LocalDateTime time) {
        return time.format(TIME);
    }
}
