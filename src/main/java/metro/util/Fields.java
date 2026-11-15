package metro.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a list of values into one line of a data file and back.
 *
 * Values are separated by '|'. Because a value may itself contain '|', a
 * backslash or a line break, those three are written in an escaped form:
 * "\|", "\\" and "\n". split() reverses exactly what join() did, so any text
 * a user types survives being saved and loaded.
 */
public final class Fields {

    public static final char SEPARATOR = '|';
    private static final char ESCAPE = '\\';

    private Fields() {
    }

    /** Builds one line from the values. A null value is written as empty. */
    public static String join(String... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            String value = values[i] == null ? "" : values[i];
            for (int j = 0; j < value.length(); j++) {
                char c = value.charAt(j);
                if (c == ESCAPE) {
                    line.append(ESCAPE).append(ESCAPE);
                } else if (c == SEPARATOR) {
                    line.append(ESCAPE).append(SEPARATOR);
                } else if (c == '\n') {
                    line.append(ESCAPE).append('n');
                } else if (c != '\r') {
                    line.append(c);
                }
            }
        }
        return line.toString();
    }

    /** Splits one line back into its values. Empty values are kept. */
    public static String[] split(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ESCAPE && i + 1 < line.length()) {
                char next = line.charAt(i + 1);
                current.append(next == 'n' ? '\n' : next);
                i++;
            } else if (c == SEPARATOR) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}
