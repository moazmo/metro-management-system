package metro.dev;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import metro.AppContext;
import metro.service.DemoData;
import metro.ui.LoginScreen;
import metro.ui.MainWindow;
import metro.ui.NavItem;
import metro.ui.Theme;

/**
 * A developer tool, not part of the application: it opens every screen for
 * every demo account and saves a picture of each one in target/screenshots.
 *
 * It is used to check the screens after a change and to produce the pictures
 * in the README. It works on a fresh temporary data folder, so it never
 * touches real data. Run it with:
 *   ./mvnw -q javafx:run -Dmetro.mainClass=metro.dev.Screenshots
 * Set the environment variable METRO_SHOTS_THEME=dark for dark pictures.
 */
public class Screenshots extends Application {

    private static final String[] USERNAMES = {"admin", "controller", "agent", "supervisor"};

    private final LinkedList<Runnable> steps = new LinkedList<>();
    private Scene scene;
    private AppContext context;
    private Path outputDir;
    private String suffix = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        boolean dark = "dark".equals(System.getenv("METRO_SHOTS_THEME"));
        suffix = dark ? "-dark" : "";
        outputDir = Path.of("target", "screenshots");
        Files.createDirectories(outputDir);
        context = new AppContext(Files.createTempDirectory("metro-screenshots"), Clock.systemDefaultZone());

        scene = new Scene(new StackPane(), 1280, 800);
        Theme.apply(scene, dark);
        stage.setTitle("Screenshots");
        stage.setScene(scene);
        stage.show();

        steps.add(() -> show(new LoginScreen(context, () -> { })));
        steps.add(() -> save("login"));
        steps.add(() -> {
            // a wrong password must show the refusal inside the card
            ((TextField) scene.lookup("#login-username")).setText("admin");
            ((TextField) scene.lookup("#login-password")).setText("wrong-password");
            ((Button) scene.lookup("#login-button")).fire();
        });
        steps.add(() -> save("login-error"));
        for (String username : USERNAMES) {
            steps.add(() -> photographAccount(username));
        }
        runNextStep();
    }

    /** Logs in as one demo account and queues a picture of every screen it can open. */
    private void photographAccount(String username) {
        context.getAuthService().logout();
        context.getAuthService().login(username, DemoData.DEMO_PASSWORD);
        MainWindow window = new MainWindow(context, () -> { });
        show(window);

        List<Runnable> forThisAccount = new ArrayList<>();
        for (NavItem item : window.getVisibleItems()) {
            String name = username + "-" + item.getLabel().toLowerCase().replace(' ', '-');
            forThisAccount.add(() -> window.open(item));
            forThisAccount.add(() -> save(name));
        }
        steps.addAll(0, forThisAccount);
    }

    private void show(Parent root) {
        scene.setRoot(root);
        Theme.apply(scene, Theme.isDark());
    }

    /** Runs the steps one after another with a short pause, so each screen is fully drawn. */
    private void runNextStep() {
        if (steps.isEmpty()) {
            Platform.exit();
            return;
        }
        steps.removeFirst().run();
        PauseTransition pause = new PauseTransition(Duration.millis(250));
        pause.setOnFinished(event -> runNextStep());
        pause.play();
    }

    /** Saves a picture of the window as a PNG file. */
    private void save(String name) {
        WritableImage image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                picture.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(picture, "png", outputDir.resolve(name + suffix + ".png").toFile());
        } catch (IOException e) {
            System.err.println("Could not save " + name + ": " + e.getMessage());
        }
    }
}
