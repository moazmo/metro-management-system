package metro.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import metro.AppContext;
import metro.exception.MetroException;

/**
 * Base class of every screen shown in the middle of the main window.
 *
 * It provides what all screens share: a title, a message banner for errors
 * and confirmations, and a helper that runs an action and turns any rule
 * violation (a MetroException) into a message for the user. Subclasses add
 * their own content with setContent(...).
 */
public abstract class Screen extends BorderPane {

    protected final AppContext context;
    private final String title;
    private final Label banner = new Label();
    private final VBox header;
    private final PauseTransition hideLater = new PauseTransition(Duration.seconds(3));

    protected Screen(AppContext context, String title) {
        this.context = context;
        this.title = title;

        Label heading = new Label(title);
        heading.getStyleClass().add("screen-title");

        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setWrapText(true);
        banner.setVisible(false);
        banner.setManaged(false);
        hideLater.setOnFinished(event -> clearMessage());

        header = new VBox(12, heading, banner);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);
        setPadding(new Insets(24, 28, 24, 28));
        getStyleClass().add("screen");
    }

    public String getTitle() {
        return title;
    }

    /** Called every time the screen is opened. Screens that show live data refresh here. */
    public void onShow() {
    }

    protected void setContent(Node content) {
        setCenter(content);
    }

    /** Shows a red message that stays until the next action. */
    protected void showError(String message) {
        showBanner(message, "banner-error", false);
    }

    /** Shows an amber message that stays until the next action. */
    protected void showWarning(String message) {
        showBanner(message, "banner-warning", false);
    }

    /** Shows a green message that disappears after three seconds. */
    protected void showInfo(String message) {
        showBanner(message, "banner-info", true);
    }

    protected void clearMessage() {
        hideLater.stop();
        banner.setVisible(false);
        banner.setManaged(false);
    }

    private void showBanner(String message, String styleClass, boolean hideAfterAWhile) {
        hideLater.stop();
        banner.setText(message);
        banner.getStyleClass().setAll("banner", styleClass);
        banner.setVisible(true);
        banner.setManaged(true);
        if (hideAfterAWhile) {
            hideLater.playFromStart();
        }
    }

    /** Asks the user to confirm something. True when they press OK. */
    protected boolean confirm(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (getScene() != null) {
            alert.initOwner(getScene().getWindow());
        }
        alert.showAndWait();
        return alert.getResult() == ButtonType.OK;
    }

    /** Opens a form dialog on top of this window. True when the user saved. */
    protected boolean showDialog(FormDialog dialog) {
        if (getScene() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        return dialog.showAndSave();
    }

    /**
     * Runs an action. If a rule is broken, the service throws a MetroException
     * and its message is shown to the user here; the screen never crashes.
     */
    protected void run(Runnable action) {
        clearMessage();
        try {
            action.run();
        } catch (MetroException e) {
            showError(e.getMessage());
        }
    }
}
