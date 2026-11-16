package metro.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import metro.exception.DataFileException;
import metro.model.AuditEntry;
import metro.util.Dates;
import metro.util.Fields;

/**
 * A record of important actions (logins, sales, refunds, changes), kept in
 * audit.log. Unlike the other files it is never rewritten: each action is
 * added as a new line at the end of the file.
 */
public class AuditLog {

    private final Path file;
    private final Clock clock;

    public AuditLog(Path file, Clock clock) {
        this.file = file;
        this.clock = clock;
    }

    /** Adds one line: time|username|action|detail. */
    public void record(String username, String action, String detail) {
        String line = Fields.join(Dates.file(LocalDateTime.now(clock).withNano(0)), username, action, detail) + "\n";
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new DataFileException("Could not save " + file.getFileName(), e);
        }
    }

    /** Every entry, oldest first. A file that does not exist yet means no entries. */
    public List<AuditEntry> readAll() {
        List<AuditEntry> entries = new ArrayList<>();
        if (!Files.exists(file)) {
            return entries;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFileException("Could not read " + file.getFileName(), e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                String[] fields = Fields.split(line);
                if (fields.length != 4) {
                    throw new IllegalArgumentException("expected 4 fields but found " + fields.length);
                }
                entries.add(new AuditEntry(Dates.parse(fields[0]), fields[1], fields[2], fields[3]));
            } catch (RuntimeException e) {
                throw new DataFileException(file.getFileName() + " line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return entries;
    }
}
