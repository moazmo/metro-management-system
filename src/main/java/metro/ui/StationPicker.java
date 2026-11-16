package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import metro.model.Station;

/**
 * A control for choosing one station out of many: a box to type part of a
 * name in, and a list that shows only the stations matching what was typed.
 * When exactly one station matches, it is chosen automatically.
 */
public class StationPicker extends VBox {

    private final List<Station> all;
    private final TextField filter = new TextField();
    private final ComboBox<Station> choice = new ComboBox<>();
    private Runnable onChange = () -> { };

    public StationPicker(List<Station> stations, String id) {
        this.all = new ArrayList<>(stations);
        setSpacing(6);
        filter.setPromptText("Type to filter…");
        choice.setId(id);
        choice.setPromptText("Choose a station");
        choice.setMaxWidth(Double.MAX_VALUE);
        choice.getItems().addAll(all);
        filter.textProperty().addListener((observable, before, after) -> applyFilter());
        choice.setOnAction(event -> onChange.run());
        getChildren().addAll(filter, choice);
    }

    private void applyFilter() {
        String wanted = filter.getText().trim().toLowerCase();
        Station selected = choice.getValue();
        List<Station> matching = new ArrayList<>();
        for (Station station : all) {
            if (wanted.isEmpty() || station.getName().toLowerCase().contains(wanted)
                    || station.getNameAr().contains(wanted)) {
                matching.add(station);
            }
        }
        choice.getItems().setAll(matching);
        if (matching.size() == 1) {
            choice.setValue(matching.get(0));
        } else if (selected != null && matching.contains(selected)) {
            choice.setValue(selected);
        }
    }

    public Station getValue() {
        return choice.getValue();
    }

    public void setValue(Station station) {
        filter.clear();
        choice.setValue(station);
    }

    /** What to do whenever another station is chosen. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }
}
