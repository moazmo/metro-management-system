package metro.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The JavaFX application: creates the main window.
 */
public class MetroApp extends Application {

    public static final String TITLE = "Metro Management System";

    @Override
    public void start(Stage stage) {
        Label label = new Label(TITLE);
        label.getStyleClass().add("title-1");
        Scene scene = new Scene(new StackPane(label), 1280, 800);
        Theme.apply(scene, false);
        stage.setTitle(TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }
}
