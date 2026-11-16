package metro.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** A dashboard tile: a large number, what it counts, and an icon. */
public class KpiTile extends VBox {

    public KpiTile(String label, String value, String icon) {
        getStyleClass().add("kpi-tile");
        Label caption = new Label(label);
        caption.getStyleClass().add("text-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, caption, spacer, Icons.of(icon, 20));
        Label number = new Label(value);
        number.getStyleClass().add("kpi-value");
        getChildren().addAll(top, number);
        setSpacing(6);
        HBox.setHgrow(this, Priority.ALWAYS);
        setMaxWidth(Double.MAX_VALUE);
    }
}
