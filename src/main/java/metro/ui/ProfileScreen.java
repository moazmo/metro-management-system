package metro.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.exception.ValidationException;
import metro.model.User;
import metro.service.UserService;

/**
 * Lets the logged-in user change their own details and password.
 * The ID, the username and the role are shown but cannot be edited.
 */
public class ProfileScreen extends Screen {

    private final UserService service;
    private final TextField fullName = new TextField();
    private final TextField phone = new TextField();
    private final TextField email = new TextField();
    private final PasswordField currentPassword = new PasswordField();
    private final PasswordField newPassword = new PasswordField();
    private final PasswordField repeatPassword = new PasswordField();

    public ProfileScreen(AppContext context) {
        super(context, "My profile");
        this.service = context.getUserService();
        User user = context.getSession().getUser();

        GridPane details = grid();
        details.addRow(0, new Label("ID"), readOnly(user.getId()));
        details.addRow(1, new Label("Username"), readOnly(user.getUsername()));
        details.addRow(2, new Label("Role"), readOnly(user.getRole().getDisplayName()));
        details.addRow(3, new Label("Full name"), fullName);
        details.addRow(4, new Label("Phone"), phone);
        details.addRow(5, new Label("Email"), email);
        fullName.setText(user.getFullName());
        phone.setText(user.getPhone());
        email.setText(user.getEmail());
        phone.setId("profile-phone");

        Button save = new Button("Save");
        save.setId("profile-save");
        save.getStyleClass().add("accent");
        save.setOnAction(event -> run(this::saveDetails));

        GridPane passwords = grid();
        passwords.addRow(0, new Label("Current password"), currentPassword);
        passwords.addRow(1, new Label("New password"), newPassword);
        passwords.addRow(2, new Label("Repeat new password"), repeatPassword);
        Button change = new Button("Change password");
        change.setOnAction(event -> run(this::changePassword));

        HBox cards = new HBox(20,
                card("Personal information", details, save),
                card("Change password", passwords, change));
        setContent(cards);
    }

    private void saveDetails() {
        service.updateOwnProfile(fullName.getText(), phone.getText(), email.getText());
        showInfo("Profile saved.");
        if (getScene() != null && getScene().getRoot() instanceof MainWindow) {
            ((MainWindow) getScene().getRoot()).refreshUser();
        }
    }

    private void changePassword() {
        // Comparing the two typed copies is a form check, not a business rule, so it is done here.
        if (!newPassword.getText().equals(repeatPassword.getText())) {
            throw new ValidationException("The two new passwords do not match");
        }
        service.changeOwnPassword(currentPassword.getText(), newPassword.getText());
        currentPassword.clear();
        newPassword.clear();
        repeatPassword.clear();
        showInfo("Password changed.");
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(140);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setMinWidth(260);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    private TextField readOnly(String value) {
        TextField field = new TextField(value);
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.getStyleClass().add("read-only");
        return field;
    }

    private VBox card(String title, GridPane grid, Button button) {
        Label heading = new Label(title);
        heading.getStyleClass().add("form-card-title");
        VBox card = new VBox(16, heading, grid, button);
        card.getStyleClass().add("form-card");
        card.setPadding(new Insets(20));
        card.setMaxHeight(VBox.USE_PREF_SIZE);
        return card;
    }
}
