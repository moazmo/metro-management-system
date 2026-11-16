package metro.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.exception.MetroException;

/**
 * The first screen: a card asking for a username and a password.
 */
public class LoginScreen extends StackPane {

    private final AppContext context;
    private final Runnable onLoggedIn;
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label error = new Label();

    public LoginScreen(AppContext context, Runnable onLoggedIn) {
        this.context = context;
        this.onLoggedIn = onLoggedIn;
        getStyleClass().add("login-screen");

        Label logo = new Label(null, Icons.of("directions_subway", 44));
        logo.getStyleClass().add("login-logo");
        Label title = new Label(MetroApp.TITLE);
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Staff sign in");
        subtitle.getStyleClass().add("text-muted");

        username.setId("login-username");
        password.setId("login-password");
        username.setPromptText("Username");
        password.setPromptText("Password");
        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        Button login = new Button("Log in");
        login.setId("login-button");
        login.getStyleClass().add("accent");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(event -> logIn());

        Label hint = new Label("Demo accounts: admin, controller, agent, supervisor");
        hint.getStyleClass().addAll("text-muted", "text-small");

        Label usernameLabel = new Label("Username");
        Label passwordLabel = new Label("Password");
        VBox fields = new VBox(6, usernameLabel, username, new Label(), passwordLabel, password);
        fields.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, logo, title, subtitle, fields, error, login, hint);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setMaxSize(380, VBox.USE_PREF_SIZE);
        getChildren().add(card);
    }

    /** Asks the service to log in; a refusal is shown under the fields. */
    private void logIn() {
        try {
            context.getAuthService().login(username.getText(), password.getText());
            onLoggedIn.run();
        } catch (MetroException e) {
            error.setText(e.getMessage());
            error.setVisible(true);
            error.setManaged(true);
            password.clear();
            password.requestFocus();
        }
    }
}
