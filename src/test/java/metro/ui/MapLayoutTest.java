package metro.ui;

import static metro.ui.MapLayout.MARGIN_LEFT;
import static metro.ui.MapLayout.MARGIN_TOP;
import static metro.ui.MapLayout.ROW_GAP;
import static metro.ui.MapLayout.SPACING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import metro.TestData;
import metro.model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fixture network: Red A B C D E · Blue F C G H · Green H E I · Island X Y. */
class MapLayoutTest {

    @TempDir
    Path dir;
    TestData data;
    MapLayout layout;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        layout = new MapLayout(List.of(data.red, data.blue, data.green, data.island));
    }

    @Test
    void stationsAreEvenlySpacedAlongTheirLine() {
        assertEquals(SPACING, layout.xOf(data.red, data.stationB) - layout.xOf(data.red, data.stationA));
        assertEquals(MARGIN_LEFT, layout.xOf(data.red, data.stationA));
        assertEquals(MARGIN_LEFT + 4 * SPACING, layout.xOf(data.red, data.stationE));
    }

    @Test
    void eachLineHasItsOwnRow() {
        assertEquals(ROW_GAP, layout.yOf(data.blue) - layout.yOf(data.red));
        assertEquals(MARGIN_TOP, layout.yOf(data.red));
        assertEquals(MARGIN_TOP + 3 * ROW_GAP, layout.yOf(data.island));
    }

    @Test
    void aLineIsShiftedSoItsFirstSharedStationLinesUp() {
        // Blue meets Red at C: C is drawn straight below C
        assertEquals(MARGIN_LEFT + 2 * SPACING, layout.xOf(data.red, data.stationC));
        assertEquals(layout.xOf(data.red, data.stationC), layout.xOf(data.blue, data.stationC));
        // Green starts at H, which it shares with Blue
        assertEquals(layout.xOf(data.blue, data.stationH), layout.xOf(data.green, data.stationH));
        assertEquals(MARGIN_LEFT + 4 * SPACING, layout.xOf(data.green, data.stationH));
    }

    @Test
    void noStationIsPushedLeftOfTheMargin() {
        MapLayout reversed = new MapLayout(List.of(data.blue, data.red));
        assertEquals(MARGIN_LEFT, reversed.xOf(data.red, data.stationA));
        assertEquals(reversed.xOf(data.red, data.stationC), reversed.xOf(data.blue, data.stationC));
        assertEquals(MARGIN_LEFT + SPACING, reversed.xOf(data.blue, data.stationF));
    }

    @Test
    void sharedStationsAreFound() {
        Set<Station> shared = new HashSet<>(layout.sharedStations());
        assertEquals(Set.of(data.stationC, data.stationH, data.stationE), shared);
    }

    @Test
    void sizeFitsTheWidestLine() {
        // Green ends furthest right: it starts at column 4 and has 3 stations
        assertTrue(layout.getWidth() >= MARGIN_LEFT + 6 * SPACING);
        assertTrue(layout.getHeight() >= MARGIN_TOP + 3 * ROW_GAP);
    }

    @Test
    void noLinesMeansAnEmptyMap() {
        MapLayout empty = new MapLayout(new ArrayList<>());
        assertTrue(empty.sharedStations().isEmpty());
        assertTrue(empty.getWidth() > 0);
    }
}
