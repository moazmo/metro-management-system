package metro.ui;

import java.util.Arrays;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import metro.model.Role;
import metro.model.User;
import metro.service.UserService;

/**
 * The form to add a staff account, or to edit the details of an existing one.
 * When editing, the role, username and password are not shown: they have
 * their own actions on the Staff users screen.
 */
public class UserFormDialog extends FormDialog {

    private final UserService service;
    private final User existing;
    private ComboBox<Role> role;
    private TextField username;
    private PasswordField password;
    private final TextField fullName;
    private final TextField phone;
    private final TextField email;

    public UserFormDialog(UserService service, User existingOrNull) {
        super(existingOrNull == null ? "Add user" : "Edit " + existingOrNull.getUsername());
        this.service = service;
        this.existing = existingOrNull;
        if (existing == null) {
            role = addChoice("Role", Arrays.asList(Role.values()), Role.TICKET_AGENT);
            username = addText("Username", "");
            password = addPassword("Password");
        }
        fullName = addText("Full name", existing == null ? "" : existing.getFullName());
        phone = addText("Phone", existing == null ? "" : existing.getPhone());
        email = addText("Email", existing == null ? "" : existing.getEmail());
    }

    @Override
    protected void save() {
        if (existing == null) {
            service.create(role.getValue(), username.getText(), password.getText(),
                    fullName.getText(), phone.getText(), email.getText());
        } else {
            service.update(existing.getId(), fullName.getText(), phone.getText(), email.getText());
        }
    }
}
