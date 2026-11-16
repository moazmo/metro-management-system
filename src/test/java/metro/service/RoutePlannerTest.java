package metro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import metro.TestData;
import metro.exception.ValidationException;
import metro.model.Journey;
import metro.model.Leg;
import metro.model.Line;
import metro.model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fixture network: Red A B C D E · Blue F C G H · Green H E I · Island X Y. */
class RoutePlannerTest {

    @TempDir
    Path dir;
    TestData data;
    RoutePlanner planner;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        planner = new RoutePlanner(data.lines);
    }

    private void assertLeg(Leg leg, Line line, Station from, Station to, int stops) {
        assertSame(line, leg.getLine());
        assertSame(from, leg.getFrom());
        assertSame(to, leg.getTo());
        assertEquals(stops, leg.getStops());
    }

    @Test
    void sameLine() {
        Journey journey = planner.plan(data.stationA, data.stationE);
        assertEquals(4, journey.getStops());
        assertEquals(0, journey.getTransfers());
        assertEquals(1, journey.getLegs().size());
        assertLeg(journey.getLegs().get(0), data.red, data.stationA, data.stationE, 4);
        assertSame(data.stationA, journey.getFrom());
        assertSame(data.stationE, journey.getTo());
    }

    @Test
    void oneChange() {
        Journey journey = planner.plan(data.stationA, data.stationG);
        assertEquals(List.of(data.stationA, data.stationB, data.stationC, data.stationG), journey.getStations());
        assertEquals(3, journey.getStops());
        assertEquals(1, journey.getTransfers());
        assertLeg(journey.getLegs().get(0), data.red, data.stationA, data.stationC, 2);
        assertLeg(journey.getLegs().get(1), data.blue, data.stationC, data.stationG, 1);
    }

    @Test
    void picksTheShorterOfTwoWays() {
        Journey journey = planner.plan(data.stationD, data.stationH);
        assertEquals(List.of(data.stationD, data.stationE, data.stationH), journey.getStations());
        assertEquals(2, journey.getStops());
        assertLeg(journey.getLegs().get(0), data.red, data.stationD, data.stationE, 1);
        assertLeg(journey.getLegs().get(1), data.green, data.stationE, data.stationH, 1);
    }

    @Test
    void reverseHasTheSameLength() {
        assertEquals(2, planner.plan(data.stationH, data.stationD).getStops());
    }

    @Test
    void twoChanges() {
        Journey journey = planner.plan(data.stationF, data.stationI);
        assertEquals(4, journey.getStops());
        assertEquals(2, journey.getTransfers());
        List<Line> used = new ArrayList<>();
        for (Leg leg : journey.getLegs()) {
            used.add(leg.getLine());
        }
        assertEquals(List.of(data.blue, data.red, data.green), used);
    }

    @Test
    void sameStationIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.plan(data.stationA, data.stationA));
        assertEquals("Origin and destination must be different", e.getMessage());
    }

    @Test
    void noRouteIsAPlainMessage() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.plan(data.stationA, data.stationX));
        assertEquals("No route between A and X", e.getMessage());
    }

    @Test
    void stationOnNoLineHasNoRoute() {
        data.loginAs(data.admin);
        Station lonely = data.stationService.create("Lonely", "");
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.plan(data.stationA, lonely));
        assertEquals("No route between A and Lonely", e.getMessage());
    }

    @Test
    void editedNetworkIsUsed() {
        data.loginAs(data.admin);
        data.lineService.update(data.red.getId(), "Red", "#D32F2F", List.of(
                data.stationA, data.stationB, data.stationC, data.stationD, data.stationE, data.stationY));
        assertEquals(6, planner.plan(data.stationA, data.stationX).getStops());
    }

    @Test
    void sameQuestionAlwaysGivesTheSameAnswer() {
        Journey first = planner.plan(data.stationF, data.stationI);
        Journey second = planner.plan(data.stationF, data.stationI);
        assertEquals(first.getStations(), second.getStations());
    }
}
