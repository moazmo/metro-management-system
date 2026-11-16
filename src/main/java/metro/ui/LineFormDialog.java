package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import metro.model.Line;
import metro.model.Station;
import metro.service.LineService;

/**
 * The form to add or edit a line: its name, its colour and the ordered list
 * of its stations. Stations are added from a choice of those not yet on the
 * line, and reordered with Move up and Move down.
 */
public class LineFormDialog extends FormDialog {

    private final LineService service;
    private final Line existing;
    private final List<Station> allStations;
    private final TextField name;
    private final ColorPicker colour = new ColorPicker();
    private final ListView<Station> onLine = new ListView<>();
    private final ComboBox<Station> toAdd = new ComboBox<>();

    public LineFormDialog(LineService service, List<Station> allStations, Line existingOrNull) {
        super(existingOrNull == null ? "Add line" : "Edit " + existingOrNull.getName());
        this.service = service;
        this.existing = existingOrNull;
        this.allStations = allStations;

        name = addText("Name", existing == null ? "" : existing.getName());
        colour.setValue(Color.web(existing == null ? "#1976D2" : existing.getColorHex()));
        addRow("Colour", colour);

        if (existing != null) {
            onLine.getItems().addAll(existing.getStations());
        }
        onLine.getStyleClass().add("station-list");

        Button add = new Button("Add", Icons.of("add", 16));
        add.setOnAction(event -> addStation());
        toAdd.setPromptText("Choose a station to add");
        toAdd.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(toAdd, Priority.ALWAYS);
        HBox addRow = new HBox(8, toAdd, add);

        Button remove = new Button("Remove", Icons.of("remove", 16));
        remove.setOnAction(event -> removeSelected());
        Button up = new Button("Move up", Icons.of("arrow_upward", 16));
        up.setOnAction(event -> move(-1));
        Button down = new Button("Move down", Icons.of("arrow_downward", 16));
        down.setOnAction(event -> move(1));
        HBox orderRow = new HBox(8, remove, up, down);

        addRow("Stations", new VBox(8, addRow, onLine, orderRow));
        refreshChoices();
    }

    /** The choice offers only the stations that are not on the line yet. */
    private void refreshChoices() {
        List<Station> free = new ArrayList<>();
        for (Station station : allStations) {
            if (!onLine.getItems().contains(station)) {
                free.add(station);
            }
        }
        toAdd.getItems().setAll(free);
        toAdd.setValue(null);
    }

    private void addStation() {
        Station chosen = toAdd.getValue();
        if (chosen != null) {
            onLine.getItems().add(chosen);
            onLine.getSelectionModel().select(chosen);
            refreshChoices();
        }
    }

    private void removeSelected() {
        Station selected = onLine.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onLine.getItems().remove(selected);
            refreshChoices();
        }
    }

    /** Moves the selected station one place up (-1) or down (+1) in the travel order. */
    private void move(int step) {
        int from = onLine.getSelectionModel().getSelectedIndex();
        int to = from + step;
        if (from < 0 || to < 0 || to >= onLine.getItems().size()) {
            return;
        }
        Station station = onLine.getItems().remove(from);
        onLine.getItems().add(to, station);
        onLine.getSelectionModel().select(to);
    }

    private String colourAsHex() {
        Color value = colour.getValue();
        return String.format("#%02X%02X%02X",
                (int) Math.round(value.getRed() * 255),
                (int) Math.round(value.getGreen() * 255),
                (int) Math.round(value.getBlue() * 255));
    }

    @Override
    protected void save() {
        List<Station> stations = new ArrayList<>(onLine.getItems());
        if (existing == null) {
            service.create(name.getText(), colourAsHex(), stations);
        } else {
            service.update(existing.getId(), name.getText(), colourAsHex(), stations);
        }
    }
}
