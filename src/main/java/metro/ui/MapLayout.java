package metro.ui;

import java.util.ArrayList;
import java.util.List;
import metro.model.Line;
import metro.model.Station;

/**
 * Works out where everything goes on the network map. Pure arithmetic: it
 * imports nothing from JavaFX, so it can be unit-tested.
 *
 * Each line is drawn as one horizontal row, its stations evenly spaced in
 * travel order. To make the map easier to read, every line after the first
 * is shifted sideways so that the first station it shares with a line
 * already placed sits straight below that station. Shared stations that
 * still do not line up are joined by a connector when the map is drawn.
 */
public class MapLayout {

    public static final double SPACING = 30;
    public static final double ROW_GAP = 120;
    public static final double MARGIN_LEFT = 150;
    public static final double MARGIN_TOP = 110;
    private static final double MARGIN_RIGHT = 90;
    private static final double MARGIN_BOTTOM = 40;

    private final List<Line> lines;
    /** How many columns each line is shifted to the right, in the same order as the lines. */
    private final int[] offsets;
    private int columns = 1;

    public MapLayout(List<Line> lines) {
        this.lines = new ArrayList<>(lines);
        this.offsets = new int[lines.size()];
        for (int i = 1; i < this.lines.size(); i++) {
            offsets[i] = alignWithEarlierLine(i);
        }
        // Shifting may have pushed a line to the left of the first column; move everything back.
        int lowest = 0;
        for (int offset : offsets) {
            lowest = Math.min(lowest, offset);
        }
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] -= lowest;
            columns = Math.max(columns, offsets[i] + this.lines.get(i).size());
        }
    }

    /** The shift that puts line i's first station shared with an earlier line under that station. */
    private int alignWithEarlierLine(int i) {
        Line line = lines.get(i);
        for (int position = 0; position < line.size(); position++) {
            Station station = line.getStations().get(position);
            for (int earlier = 0; earlier < i; earlier++) {
                int there = lines.get(earlier).indexOf(station);
                if (there >= 0) {
                    return offsets[earlier] + there - position;
                }
            }
        }
        return 0; // the line meets no earlier line: start it at the left
    }

    private int rowOf(Line line) {
        int row = lines.indexOf(line);
        if (row < 0) {
            throw new IllegalArgumentException(line.getName() + " is not on this map");
        }
        return row;
    }

    /** Horizontal position of a station on the row of a line. */
    public double xOf(Line line, Station station) {
        int row = rowOf(line);
        return MARGIN_LEFT + (offsets[row] + line.indexOf(station)) * SPACING;
    }

    /** Vertical position of a line's row. */
    public double yOf(Line line) {
        return MARGIN_TOP + rowOf(line) * ROW_GAP;
    }

    public List<Line> getLines() {
        return new ArrayList<>(lines);
    }

    /** Every station that is on two or more of the lines (each listed once). */
    public List<Station> sharedStations() {
        List<Station> shared = new ArrayList<>();
        for (Line line : lines) {
            for (Station station : line.getStations()) {
                if (!shared.contains(station) && linesOf(station).size() >= 2) {
                    shared.add(station);
                }
            }
        }
        return shared;
    }

    /** The lines of this map that a station is on, top row first. */
    public List<Line> linesOf(Station station) {
        List<Line> found = new ArrayList<>();
        for (Line line : lines) {
            if (line.contains(station)) {
                found.add(line);
            }
        }
        return found;
    }

    public double getWidth() {
        return MARGIN_LEFT + (columns - 1) * SPACING + MARGIN_RIGHT;
    }

    public double getHeight() {
        return MARGIN_TOP + Math.max(0, lines.size() - 1) * ROW_GAP + MARGIN_BOTTOM;
    }
}
