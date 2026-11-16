package metro.dev;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import metro.AppContext;
import metro.util.Dates;
import metro.model.Line;
import metro.model.Station;
import metro.model.TicketType;
import metro.model.Trip;
import metro.service.DemoData;
import metro.ui.LineFormDialog;
import metro.ui.LoginScreen;
import metro.ui.MainWindow;
import metro.ui.NavItem;
import metro.ui.ReportScreen;
import metro.ui.Theme;
import metro.ui.TripFormDialog;
import metro.ui.UserFormDialog;

/**
 * A developer tool, not part of the application: it opens every screen for
 * every demo account and saves a picture of each one in target/screenshots.
 * It also performs a few scripted actions (typing into fields, pressing
 * buttons) and prints a CHECK line saying whether each one worked.
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
    private Stage stage;
    private Scene scene;
    private AppContext context;
    private Path dataDir;
    private Path outputDir;
    private String suffix = "";
    private Dialog<?> openDialog;
    private Trip conflictTarget;
    private String scriptedTicketId;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        boolean dark = "dark".equals(System.getenv("METRO_SHOTS_THEME"));
        suffix = dark ? "-dark" : "";
        outputDir = Path.of("target", "screenshots");
        Files.createDirectories(outputDir);
        dataDir = Files.createTempDirectory("metro-screenshots");
        context = new AppContext(dataDir, Clock.systemDefaultZone());

        scene = new Scene(new StackPane(), 1280, 800);
        Theme.apply(scene, dark);
        stage.setTitle("Screenshots");
        stage.setScene(scene);
        stage.show();

        steps.add(() -> show(new LoginScreen(context, () -> { })));
        steps.add(() -> save("login"));
        steps.add(() -> {
            // a wrong password must show the refusal inside the card
            text("#login-username").setText("admin");
            text("#login-password").setText("wrong-password");
            button("#login-button").fire();
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
        addScriptedChecks(username, window, forThisAccount);
        steps.addAll(0, forThisAccount);
    }

    /** Extra actions for some accounts: things a person would click through by hand. */
    private void addScriptedChecks(String username, MainWindow window, List<Runnable> list) {
        if (username.equals("admin")) {
            list.add(() -> showDialog(new UserFormDialog(context.getUserService(), null)));
            list.add(() -> pressDialogSave());
            list.add(() -> {
                check("a refused save keeps the dialog open", openDialog.isShowing());
                check("a refused save stores nothing", !fileHasLine("users.txt", "USR-005"));
                saveDialog("dialog-add-user");
            });
        }
        if (username.equals("admin")) {
            list.add(() -> window.open(window.findItem("Trip report")));
            list.add(() -> {
                ((ComboBox<String>) scene.lookup("#report-status")).setValue("Delayed");
                button("#report-generate").fire();
            });
            list.add(() -> {
                boolean allDelayed = !reportTable().getItems().isEmpty();
                for (String[] row : reportTable().getItems()) {
                    allDelayed = allDelayed && row[7].equals("Delayed");
                }
                check("the trip report filtered on Delayed shows only delayed trips", allDelayed);
                ((DatePicker) scene.lookup("#report-from")).setValue(LocalDate.now().plusDays(3));
                button("#report-generate").fire();
            });
            list.add(() -> {
                check("a start date after the end date is refused with a message",
                        bannerText().equals("The start date must not be after the end date"));
                save("admin-trip-report-refused");
                window.open(window.findItem("Ticket report"));
            });
            list.add(() -> {
                try {
                    Path csv = Files.createTempFile("ticket-report", ".csv");
                    ((ReportScreen) scene.lookup(".screen")).exportTo(csv);
                    List<String> exported = Files.readAllLines(csv, StandardCharsets.UTF_8);
                    check("the ticket report exports a header and rows",
                            exported.get(0).equals("From,To,Tickets,Revenue") && exported.size() > 1);
                } catch (IOException e) {
                    check("the ticket report exports a header and rows", false);
                }
                window.open(window.findItem("Revenue report"));
                ((DatePicker) scene.lookup("#report-from")).setValue(LocalDate.of(2020, 1, 1));
                ((DatePicker) scene.lookup("#report-to")).setValue(LocalDate.of(2020, 1, 3));
                button("#report-generate").fire();
            });
            list.add(() -> {
                check("a period with no sales says so", scene.lookup(".chart-area").lookup(".label") != null
                        && reportTable().getItems().size() == 4);
                save("admin-revenue-report-empty");
            });
        }
        if (username.equals("admin")) {
            list.add(() -> window.open(window.findItem("Stations")));
            list.add(() -> text("#crud-search").setText("sadat"));
            list.add(() -> {
                check("searching stations for 'sadat' leaves one row", table().getItems().size() == 1);
                save("admin-stations-search");
            });
            list.add(() -> {
                Line second = context.getLineService().listAll().get(1);
                showDialog(new LineFormDialog(context.getLineService(),
                        context.getStationService().listAll(), second));
            });
            list.add(() -> saveDialog("dialog-edit-line"));
        }
        if (username.equals("controller")) {
            list.add(() -> showDialog(new TripFormDialog(context, null)));
            list.add(() -> saveDialog("dialog-new-trip"));
            list.add(() -> {
                // editing a trip onto a time when its train is busy must be refused inside the form
                Trip first = context.getTripService().listAll().get(0);
                Trip sameTrainLater = null;
                for (Trip other : context.getTripService().listAll()) {
                    if (sameTrainLater == null && other != first && other.getTrain().equals(first.getTrain())
                            && !other.getStatus().isFinal()) {
                        sameTrainLater = other;
                    }
                }
                TripFormDialog dialog = new TripFormDialog(context, sameTrainLater);
                showDialog(dialog);
                conflictTarget = sameTrainLater;
            });
            list.add(() -> {
                // find another unfinished trip of the same train and move this one onto it
                Trip clashWith = null;
                for (Trip other : context.getTripService().listAll()) {
                    if (other != conflictTarget && other.getTrain().equals(conflictTarget.getTrain())
                            && !other.getStatus().isFinal()) {
                        clashWith = other;
                    }
                }
                DatePicker date = (DatePicker) dialogNode(".date-picker");
                date.setValue(clashWith.getDeparture().toLocalDate());
                List<TextField> fields = dialogTextFields();
                fields.get(fields.size() - 2).setText(Dates.time(clashWith.getDeparture()));
                pressDialogSave();
            });
            list.add(() -> {
                check("a conflicting trip edit is refused inside the form", openDialog.isShowing());
                saveDialog("dialog-trip-conflict");
            });
        }
        if (username.equals("agent")) {
            list.add(() -> window.open(window.findItem("Sell ticket")));
            list.add(() -> {
                stationChoice("#sell-from").setValue(station("Helwan"));
                stationChoice("#sell-to").setValue(station("Cairo University"));
            });
            list.add(() -> {
                check("choosing a journey shows a total fare", scene.lookup("#sell-total") != null);
                save("agent-sell-ticket-quote");
                button("#sell-button").fire();
            });
            list.add(() -> {
                scriptedTicketId = context.getTicketService().mySalesToday().get(0).getId();
                check("a sale is written to tickets.txt", fileHasLine("tickets.txt", scriptedTicketId, "|VALID|"));
                save("agent-sell-ticket-sold");
            });
            list.add(() -> window.open(window.findItem("Validate")));
            list.add(() -> {
                text("#validate-code").setText(scriptedTicketId.toLowerCase());
                button("#validate-button").fire();
            });
            list.add(() -> {
                check("a validated ticket is marked USED in tickets.txt",
                        fileHasLine("tickets.txt", scriptedTicketId, "|USED|"));
                save("agent-validate-accepted");
                text("#validate-code").setText(scriptedTicketId);
                button("#validate-button").fire();
            });
            list.add(() -> save("agent-validate-rejected"));
        }
        if (username.equals("supervisor")) {
            list.add(() -> {
                scriptedTicketId = context.getTicketService()
                        .sell(station("Helwan"), station("Sadat"), TicketType.STUDENT, 1).get(0).getId();
                window.open(window.findItem("Refunds"));
            });
            list.add(() -> {
                text("#refund-code").setText(scriptedTicketId);
                button("#refund-find").fire();
            });
            list.add(() -> {
                check("a same-day unused ticket is offered a refund",
                        scene.lookup("#refund-button") != null && !scene.lookup("#refund-button").isDisabled());
                save("supervisor-refunds-eligible");
            });
        }
        if (username.equals("agent")) {
            list.add(() -> window.open(window.findItem("My profile")));
            list.add(() -> {
                text("#profile-phone").setText("0123456789");
                button("#profile-save").fire();
            });
            list.add(() -> {
                check("profile change is written to users.txt", fileHasLine("users.txt", "|agent|", "|0123456789|"));
                save("agent-my-profile-saved");
            });
        }
    }

    // ----- small helpers for the scripted actions -----

    private TextField text(String id) {
        return (TextField) scene.lookup(id);
    }

    private ComboBox<Station> stationChoice(String id) {
        return (ComboBox<Station>) scene.lookup(id);
    }

    private Station station(String name) {
        for (Station station : context.getStationService().listAll()) {
            if (station.getName().equals(name)) {
                return station;
            }
        }
        throw new IllegalStateException("No station named " + name);
    }

    private javafx.scene.Node dialogNode(String selector) {
        return openDialog.getDialogPane().lookup(selector);
    }

    /** The plain text fields of the open dialog, top to bottom (the date picker's own field is left out). */
    private List<TextField> dialogTextFields() {
        List<TextField> fields = new ArrayList<>();
        for (javafx.scene.Node node : openDialog.getDialogPane().lookupAll(".text-field")) {
            if (node instanceof TextField && !(node.getParent() instanceof DatePicker)
                    && !(node.getParent() instanceof ComboBox)) {
                fields.add((TextField) node);
            }
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> reportTable() {
        return (TableView<String[]>) scene.lookup("#report-table");
    }

    private String bannerText() {
        javafx.scene.Node banner = scene.lookup(".banner");
        return banner != null && banner.isVisible() ? ((javafx.scene.control.Label) banner).getText() : "";
    }

    private TableView<?> table() {
        return (TableView<?>) scene.lookup("#crud-table");
    }

    private Button button(String id) {
        return (Button) scene.lookup(id);
    }

    private void check(String what, boolean ok) {
        System.out.println((ok ? "CHECK OK      " : "CHECK FAILED  ") + what);
    }

    /** True when some line of a data file contains all the given pieces of text. */
    private boolean fileHasLine(String fileName, String... parts) {
        try {
            for (String line : Files.readAllLines(dataDir.resolve(fileName), StandardCharsets.UTF_8)) {
                boolean all = true;
                for (String part : parts) {
                    all = all && line.contains(part);
                }
                if (all) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private void show(Parent root) {
        scene.setRoot(root);
        Theme.apply(scene, Theme.isDark());
    }

    private void showDialog(Dialog<?> dialog) {
        openDialog = dialog;
        dialog.initOwner(stage);
        dialog.show();
    }

    /** Presses the Save button of the dialog that is open. */
    private void pressDialogSave() {
        for (ButtonType type : openDialog.getDialogPane().getButtonTypes()) {
            if (type.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                ((Button) openDialog.getDialogPane().lookupButton(type)).fire();
            }
        }
    }

    private void saveDialog(String name) {
        write(openDialog.getDialogPane().getScene().snapshot(null), name);
        openDialog.close();
        openDialog = null;
    }

    /** Runs the steps one after another with a short pause, so each screen is fully drawn. */
    private void runNextStep() {
        if (steps.isEmpty()) {
            Platform.exit();
            return;
        }
        try {
            steps.removeFirst().run();
        } catch (RuntimeException e) {
            System.out.println("CHECK FAILED  a scripted step threw " + e);
        }
        PauseTransition pause = new PauseTransition(Duration.millis(250));
        pause.setOnFinished(event -> runNextStep());
        pause.play();
    }

    private void save(String name) {
        write(scene.snapshot(null), name);
    }

    /** Saves a picture as a PNG file. */
    private void write(WritableImage image, String name) {
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
            System.out.println("CHECK FAILED  could not save " + name + ": " + e.getMessage());
        }
    }
}
