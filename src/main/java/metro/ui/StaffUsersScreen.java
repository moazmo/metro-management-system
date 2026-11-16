package metro.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import metro.AppContext;
import metro.model.Role;
import metro.model.User;
import metro.service.UserService;

/**
 * Admin screen for staff accounts: add, edit, change role, reset password,
 * activate and deactivate. Accounts are never deleted, because tickets and
 * log entries refer to them.
 */
public class StaffUsersScreen extends CrudScreen<User> {

    private final UserService service;

    public StaffUsersScreen(AppContext context) {
        super(context, "Staff users", "Add user");
        this.service = context.getUserService();
        setEmptyText("No users match.");
    }

    @Override
    protected List<User> loadItems(String searchText) {
        return service.search(searchText);
    }

    @Override
    protected void buildColumns(TableView<User> table) {
        addColumn(table, "ID", user -> user.getId());
        addColumn(table, "Username", user -> user.getUsername());
        addColumn(table, "Full name", user -> user.getFullName());
        addColumn(table, "Role", user -> user.getRole().getDisplayName());
        addBadgeColumn(table, "Status", user -> user.isActive()
                ? new StatusBadge("Active", StatusBadge.Tone.GOOD)
                : new StatusBadge("Inactive", StatusBadge.Tone.NEUTRAL));
    }

    @Override
    protected void openForm(User existingOrNull) {
        if (showDialog(new UserFormDialog(service, existingOrNull))) {
            refresh();
            showInfo(existingOrNull == null ? "User added." : "User updated.");
        }
    }

    @Override
    protected List<Button> rowActions(User selected) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(action("Edit", "edit", () -> openForm(selected)));
        buttons.add(action("Change role", "supervisor_account", () -> {
            if (showDialog(new RoleDialog(service, selected))) {
                showInfo("Role changed.");
            }
        }));
        buttons.add(action("Reset password", "vpn_key", () -> {
            if (showDialog(new PasswordDialog(service, selected))) {
                showInfo("Password reset.");
            }
        }));
        if (selected.isActive()) {
            buttons.add(action("Deactivate", "block", () -> {
                if (confirm("Deactivate " + selected.getUsername() + "? They will not be able to log in.")) {
                    service.setActive(selected.getId(), false);
                    showInfo("User deactivated.");
                }
            }));
        } else {
            buttons.add(action("Activate", "check_circle", () -> {
                service.setActive(selected.getId(), true);
                showInfo("User activated.");
            }));
        }
        return buttons;
    }

    /** Small form to choose another role for an account. */
    private static class RoleDialog extends FormDialog {
        private final UserService service;
        private final User user;
        private final ComboBox<Role> role;

        RoleDialog(UserService service, User user) {
            super("Change role of " + user.getUsername());
            this.service = service;
            this.user = user;
            this.role = addChoice("Role", Arrays.asList(Role.values()), user.getRole());
        }

        @Override
        protected void save() {
            service.changeRole(user.getId(), role.getValue());
        }
    }

    /** Small form to give an account a new password. */
    private static class PasswordDialog extends FormDialog {
        private final UserService service;
        private final User user;
        private final PasswordField password;

        PasswordDialog(UserService service, User user) {
            super("Reset password of " + user.getUsername());
            this.service = service;
            this.user = user;
            this.password = addPassword("New password");
        }

        @Override
        protected void save() {
            service.resetPassword(user.getId(), password.getText());
        }
    }
}
