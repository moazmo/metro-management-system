package metro.service.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import metro.exception.DataFileException;
import metro.exception.ValidationException;

/**
 * Base class of every report.
 *
 * A report covers a range of dates and can describe itself as a table:
 * column names and rows of text. Each kind of report (trips, tickets,
 * revenue) fills in those two methods in its own way. What is the same for
 * all of them lives here: checking the date range and exporting the table
 * to a CSV file. The export works for any report because it only uses
 * getColumns() and getRows().
 */
public abstract class Report {

    private final String title;
    private final LocalDate from;
    private final LocalDate to;

    /**
     * Rule: a report needs a start date and an end date, and the start must
     * not be after the end.
     */
    protected Report(String title, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Choose a start date and an end date");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("The start date must not be after the end date");
        }
        this.title = title;
        this.from = from;
        this.to = to;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    /** The column names of the report's table. */
    public abstract List<String> getColumns();

    /** The rows of the report's table, as text, in the order they are shown. */
    public abstract List<String[]> getRows();

    /** True when the moment falls on a day of the report's range; both end days count fully. */
    protected final boolean inRange(LocalDateTime time) {
        LocalDate day = time.toLocalDate();
        return !day.isBefore(from) && !day.isAfter(to);
    }

    /** Writes the table to a CSV file: the column names, then one line per row. */
    public final void exportCsv(Path file) {
        StringBuilder text = new StringBuilder();
        text.append(csvLine(getColumns().toArray(new String[0]))).append('\n');
        for (String[] row : getRows()) {
            text.append(csvLine(row)).append('\n');
        }
        try {
            Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DataFileException("Could not save " + file.getFileName(), e);
        }
    }

    /**
     * One line of a CSV file. A value that contains a comma, a double quote
     * or a line break is wrapped in double quotes, and quotes inside it are doubled.
     */
    private String csvLine(String[] values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            String value = values[i] == null ? "" : values[i];
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                line.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                line.append(value);
            }
        }
        return line.toString();
    }
}
