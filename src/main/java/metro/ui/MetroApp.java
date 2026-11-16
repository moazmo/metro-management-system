package metro.ui;

import java.nio.file.Path;
import java.time.Clock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import metro.AppContext;
import metro.exception.DataFileException;

/**
 * The JavaFX application: creates the window, builds the AppContext, and
 * switches between the login screen and the main window.
 */
public class MetroApp extends Application {

    public static final String TITLE = "Metro Management System";

    private Scene scene;
    private AppContext context;

    @Override
    public void start(Stage stage) {
        scene = new Scene(new StackPane(), 1280, 800);
        Theme.apply(scene, false);

        try {
            context = new AppContext(Path.of("data"), Clock.systemDefaultZone());
        } catch (DataFileException e) {
            // A damaged data file is explained in plain words; the program then stops.
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.CLOSE);
            alert.setTitle(TITLE);
            alert.setHeaderText("A data file could not be read");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // the last chosen look, light or dark, is kept in settings.txt
        Theme.apply(scene, "dark".equals(context.getSettings().get("theme", "light")));
        stage.setTitle(TITLE);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        setRoot(new LoginScreen(context, this::showMainWindow));
    }

    private void showMainWindow() {
        setRoot(new MainWindow(context, this::logOut, this::startOver));
    }

    /** After a reset: build everything again from the (now fresh) data folder and show the login screen. */
    private void startOver() {
        context = new AppContext(context.getDataDir(), context.getClock());
        showLogin();
    }

    private void logOut() {
        context.getAuthService().logout();
        showLogin();
    }

    private void setRoot(Parent root) {
        scene.setRoot(root);
        Theme.apply(scene, Theme.isDark());
    }
}
