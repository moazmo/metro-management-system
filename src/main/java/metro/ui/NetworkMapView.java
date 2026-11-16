package metro.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import metro.model.Journey;
import metro.model.Leg;
import metro.model.Line;
import metro.model.Station;

/**
 * Draws the metro network from the lines that exist right now, so the
 * picture always follows what an admin has entered. Positions come from
 * MapLayout; this class only turns them into shapes.
 *
 *   - each line is a thick stroke in its colour, one row per line
 *   - a station is a dot; an interchange is a larger ringed dot; a closed
 *     station is grey and its name is struck through
 *   - a station on several rows is joined across them by a dashed connector
 *
 * Clicking a station tells the listener which station it was. A journey can
 * be highlighted: everything else fades and the route stays bright.
 */
public class NetworkMapView extends Pane {

    private static final Color FADED_GREY = Color.web("#8C959F");

    private final Group base = new Group();
    private final Group overlay = new Group();
    private final Group selection = new Group();
    /** The dot of every station on every row, by "lineId|stationId". */
    private final Map<String, Node> dots = new HashMap<>();
    /** The names drawn for each station (a station drawn straight below its own name is not named again). */
    private final Map<Station, List<Node>> labels = new HashMap<>();
    /** Where each station's name has been drawn, to avoid naming it twice in the same place. */
    private final Map<Station, Double> labelledAt = new HashMap<>();
    private MapLayout layout;
    private Consumer<Station> onStationClicked = station -> { };

    public NetworkMapView(List<Line> lines) {
        getStyleClass().add("network-map");
        getChildren().addAll(base, overlay, selection);
        rebuild(lines);
    }

    /** Draws the map again from these lines. */
    public void rebuild(List<Line> lines) {
        layout = new MapLayout(lines);
        base.getChildren().clear();
        overlay.getChildren().clear();
        selection.getChildren().clear();
        dots.clear();
        labels.clear();
        labelledAt.clear();
        drawConnectors();
        for (Line line : layout.getLines()) {
            drawRow(line);
        }
        setPrefSize(layout.getWidth(), layout.getHeight());
        setMinSize(layout.getWidth(), layout.getHeight());
    }

    public void setOnStationClicked(Consumer<Station> handler) {
        this.onStationClicked = handler;
    }

    // ----- drawing -----

    /**
     * Dashed links between the dots of one station that appears on several
     * rows. Two dots on neighbouring rows that already sit one above the
     * other need no link: the eye sees they belong together.
     */
    private void drawConnectors() {
        List<Line> rows = layout.getLines();
        for (Station station : layout.sharedStations()) {
            List<Line> onRows = layout.linesOf(station);
            for (int i = 0; i + 1 < onRows.size(); i++) {
                Line upper = onRows.get(i);
                Line lower = onRows.get(i + 1);
                boolean neighbours = rows.indexOf(lower) - rows.indexOf(upper) == 1;
                if (neighbours && layout.xOf(upper, station) == layout.xOf(lower, station)) {
                    continue;
                }
                javafx.scene.shape.Line link = connector(upper, lower, station);
                link.getStyleClass().add("map-connector");
                base.getChildren().add(link);
            }
        }
    }

    private javafx.scene.shape.Line connector(Line upper, Line lower, Station station) {
        javafx.scene.shape.Line link = new javafx.scene.shape.Line(
                layout.xOf(upper, station), layout.yOf(upper), layout.xOf(lower, station), layout.yOf(lower));
        link.getStrokeDashArray().addAll(5.0, 5.0);
        link.setStrokeWidth(1.5);
        return link;
    }

    private void drawRow(Line line) {
        double y = layout.yOf(line);
        Color colour = Color.web(line.getColorHex());

        Text name = new Text(8, y - 6, line.getName());
        name.setWrappingWidth(MapLayout.MARGIN_LEFT - 30);
        name.setFont(Font.font(null, FontWeight.BOLD, 12));
        name.setFill(colour);
        base.getChildren().add(name);

        javafx.scene.shape.Line stroke = new javafx.scene.shape.Line(
                layout.xOf(line, line.getFirstStation()), y, layout.xOf(line, line.getLastStation()), y);
        stroke.setStroke(colour);
        stroke.setStrokeWidth(6);
        stroke.setStrokeLineCap(StrokeLineCap.ROUND);
        base.getChildren().add(stroke);

        for (Station station : line.getStations()) {
            drawStation(line, station, colour);
        }
    }

    private void drawStation(Line line, Station station, Color colour) {
        double x = layout.xOf(line, station);
        double y = layout.yOf(line);
        boolean interchange = layout.linesOf(station).size() >= 2;

        Circle dot = new Circle(x, y, interchange ? 7 : 5);
        if (!station.isOpen()) {
            dot.setFill(FADED_GREY);
            dot.getStyleClass().add("map-dot-closed");
        } else if (interchange) {
            dot.getStyleClass().add("map-interchange");
        } else {
            dot.setFill(colour);
            dot.getStyleClass().add("map-dot");
        }

        Text label = new Text(x + 7, y - 11, station.getName());
        label.setFont(Font.font(11));
        label.getStyleClass().add("map-label");
        label.getTransforms().add(new Rotate(-45, x, y));
        if (!station.isOpen()) {
            label.setStrikethrough(true);
            label.getStyleClass().add("map-label-closed");
        }

        boolean alreadyNamedHere = labelledAt.containsKey(station) && labelledAt.get(station) == x;
        String tip = station.getName() + (station.getNameAr().isEmpty() ? "" : "  ·  " + station.getNameAr())
                + (station.isOpen() ? "" : "  (closed)");
        for (Node node : new Node[] {dot, label}) {
            node.setCursor(Cursor.HAND);
            node.setOnMouseClicked(event -> onStationClicked.accept(station));
            Tooltip.install(node, new Tooltip(tip));
        }
        base.getChildren().add(dot);
        dots.put(key(line, station), dot);
        if (!alreadyNamedHere) {
            base.getChildren().add(label);
            labelledAt.put(station, x);
            if (!labels.containsKey(station)) {
                labels.put(station, new ArrayList<>());
            }
            labels.get(station).add(label);
        }
    }

    private String key(Line line, Station station) {
        return line.getId() + "|" + station.getId();
    }

    // ----- selection and highlight -----

    /** Rings the chosen origin and destination. Either may be null. */
    public void setSelection(Station from, Station to) {
        selection.getChildren().clear();
        ring(from);
        ring(to);
    }

    private void ring(Station station) {
        if (station == null) {
            return;
        }
        for (Line line : layout.linesOf(station)) {
            Circle ring = new Circle(layout.xOf(line, station), layout.yOf(line), 11);
            ring.getStyleClass().add("map-selection");
            ring.setMouseTransparent(true);
            selection.getChildren().add(ring);
        }
    }

    /** Fades the whole map except the journey's route. Null brings the whole map back. */
    public void highlight(Journey journey) {
        overlay.getChildren().clear();
        for (Node node : base.getChildren()) {
            node.setOpacity(journey == null ? 1.0 : 0.22);
        }
        if (journey == null) {
            return;
        }
        List<Leg> legs = journey.getLegs();
        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            Line line = leg.getLine();
            double y = layout.yOf(line);
            javafx.scene.shape.Line bright = new javafx.scene.shape.Line(
                    layout.xOf(line, leg.getFrom()), y, layout.xOf(line, leg.getTo()), y);
            bright.setStroke(Color.web(line.getColorHex()));
            bright.setStrokeWidth(10);
            bright.setStrokeLineCap(StrokeLineCap.ROUND);
            bright.setMouseTransparent(true);
            overlay.getChildren().add(bright);

            // the stations passed on this leg come back to full strength
            int start = Math.min(line.indexOf(leg.getFrom()), line.indexOf(leg.getTo()));
            int end = Math.max(line.indexOf(leg.getFrom()), line.indexOf(leg.getTo()));
            for (int position = start; position <= end; position++) {
                Station passed = line.getStations().get(position);
                Node dot = dots.get(key(line, passed));
                dot.setOpacity(1.0);
                dot.toFront();
                if (labels.containsKey(passed)) {
                    for (Node label : labels.get(passed)) {
                        label.setOpacity(1.0);
                    }
                }
            }
            if (i > 0 && !legs.get(i - 1).getLine().equals(line)) {
                javafx.scene.shape.Line change = connector(legs.get(i - 1).getLine(), line, leg.getFrom());
                change.getStyleClass().add("map-change");
                change.setStrokeWidth(3);
                change.setMouseTransparent(true);
                overlay.getChildren().add(change);
            }
        }
    }
}
