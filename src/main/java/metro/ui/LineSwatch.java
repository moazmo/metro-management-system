package metro.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import metro.model.Line;

/**
 * A small coloured dot followed by a line's name. Used wherever a line is
 * shown in a table or a list, so a line always looks the same.
 */
public class LineSwatch extends HBox {

    public LineSwatch(Line line) {
        this(line, line.getName());
    }

    public LineSwatch(Line line, String text) {
        Circle dot = new Circle(6, Color.web(line.getColorHex()));
        getChildren().addAll(dot, new Label(text));
        getStyleClass().add("line-swatch");
    }
}
