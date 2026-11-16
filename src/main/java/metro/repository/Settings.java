package metro.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import metro.exception.DataFileException;

/**
 * Small preferences of the application, such as light or dark mode, kept in
 * settings.txt as lines of the form key=value. Lines that do not look like
 * that are ignored, so a damaged settings file never stops the program.
 */
public class Settings {

    private final Path file;
    private final Map<String, String> values = new LinkedHashMap<>();

    public Settings(Path file) {
        this.file = file;
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                int equals = line.indexOf('=');
                if (equals > 0) {
                    values.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            }
        } catch (IOException e) {
            // Preferences are not important enough to stop the program: start with none.
            values.clear();
        }
    }

    /** The stored value, or the fallback when there is none. */
    public String get(String key, String fallback) {
        String value = values.get(key);
        return value == null ? fallback : value;
    }

    /** Stores a value and writes the file at once. */
    public void set(String key, String value) {
        values.put(key, value);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFileException("Could not save " + file.getFileName(), e);
        }
    }
}
