package metro.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import metro.exception.DataFileException;
import metro.exception.NotFoundException;
import metro.model.Identifiable;
import metro.util.Fields;

/**
 * A repository that keeps its records in one text file.
 *
 * This class does everything that is the same for every record type:
 * reading the file, writing it back, finding, adding, replacing and deleting
 * records, and giving out new ids. What differs per type is only how one
 * line becomes one object and back, so those two steps are abstract methods
 * that each subclass fills in (parse and format).
 *
 * File layout:
 *   #next=3                      the next id number to give out
 *   # id|name|nameAr|status      the column names, for people reading the file
 *   STN-001|Helwan|حلوان|OPEN    one record per line
 */
public abstract class FileRepository<T extends Identifiable> implements Repository<T> {

    private static final String NEXT_PREFIX = "#next=";

    private final Path file;
    private final String label;
    private final String idPrefix;
    private final int idDigits;
    private final String header;
    private final int columnCount;
    private final List<T> items = new ArrayList<>();
    private int nextNumber = 1;

    /**
     * @param file     where the records are stored
     * @param label    what one record is called in messages, for example "Station"
     * @param idPrefix first part of every id, for example "STN"
     * @param idDigits how many digits the number part of an id has
     * @param header   the column names joined by '|'
     */
    protected FileRepository(Path file, String label, String idPrefix, int idDigits, String header) {
        this.file = file;
        this.label = label;
        this.idPrefix = idPrefix;
        this.idDigits = idDigits;
        this.header = header;
        this.columnCount = header.split("\\|").length;
    }

    /** Turns the fields of one line into one object. */
    protected abstract T parse(String[] fields);

    /** Turns one object into the fields of one line. */
    protected abstract String[] format(T item);

    /** Reads the file into memory. A file that does not exist yet means no records. */
    public void load() {
        items.clear();
        nextNumber = 1;
        if (!Files.exists(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFileException("Could not read " + fileName(), e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("﻿")) {
                line = line.substring(1); // some editors add this invisible mark at the start of a file
            }
            if (line.startsWith(NEXT_PREFIX)) {
                nextNumber = readNextNumber(line, i + 1);
            } else if (!line.isBlank() && !line.startsWith("#")) {
                items.add(parseLine(line, i + 1));
            }
        }
        // Even if the counter line was lost, never give out an id that is in use.
        for (T item : items) {
            int used = numberOf(item.getId());
            if (used >= nextNumber) {
                nextNumber = used + 1;
            }
        }
    }

    private int readNextNumber(String line, int lineNumber) {
        try {
            return Integer.parseInt(line.substring(NEXT_PREFIX.length()).trim());
        } catch (NumberFormatException e) {
            throw new DataFileException(fileName() + " line " + lineNumber + ": the id counter is not a number", e);
        }
    }

    private T parseLine(String line, int lineNumber) {
        try {
            String[] fields = Fields.split(line);
            if (fields.length != columnCount) {
                throw new IllegalArgumentException(
                        "expected " + columnCount + " fields but found " + fields.length);
            }
            return parse(fields);
        } catch (RuntimeException e) {
            throw new DataFileException(fileName() + " line " + lineNumber + ": " + reason(e), e);
        }
    }

    /** A short, readable reason for a line that could not be understood. */
    private String reason(RuntimeException e) {
        String message = e.getMessage();
        if (message == null) {
            return "the line is damaged";
        }
        if (message.startsWith("No enum constant")) {
            return "unknown value " + message.substring(message.lastIndexOf('.') + 1);
        }
        if (e instanceof NumberFormatException) {
            return "a number was expected (" + message + ")";
        }
        return message;
    }

    /** The number part of an id such as "STN-014"; 0 when it has none. */
    private int numberOf(String id) {
        int dash = id.lastIndexOf('-');
        try {
            return Integer.parseInt(id.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Writes all records to a temporary file, then swaps it in place of the real one. */
    private void flush() {
        StringBuilder text = new StringBuilder();
        text.append(NEXT_PREFIX).append(nextNumber).append('\n');
        text.append("# ").append(header).append('\n');
        for (T item : items) {
            text.append(Fields.join(format(item))).append('\n');
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(temporary, text.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DataFileException("Could not save " + fileName(), e);
        }
    }

    protected final String fileName() {
        return file.getFileName().toString();
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(items);
    }

    @Override
    public T findById(String id) {
        for (T item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public T require(String id) {
        T item = findById(id);
        if (item == null) {
            throw new NotFoundException(label + " " + id + " was not found");
        }
        return item;
    }

    @Override
    public void save(T item) {
        int position = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                position = i;
            }
        }
        if (position >= 0) {
            items.set(position, item);
        } else {
            items.add(item);
        }
        flush();
    }

    @Override
    public void delete(String id) {
        T item = require(id);
        items.remove(item);
        flush();
    }

    @Override
    public String nextId() {
        String id = String.format("%s-%0" + idDigits + "d", idPrefix, nextNumber);
        nextNumber++;
        return id;
    }
}
