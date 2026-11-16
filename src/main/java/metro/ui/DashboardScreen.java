package metro.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.model.User;

/**
 * The screen every user lands on after logging in.
 */
public class DashboardScreen extends Screen {

    public DashboardScreen(AppContext context) {
        super(context, "Dashboard");
        User user = context.getSession().getUser();
        Label welcome = new Label("Welcome, " + user.getFullName());
        welcome.getStyleClass().add("title-3");
        Label role = new Label("You are signed in as " + user.getRole().getDisplayName() + ".");
        role.getStyleClass().add("text-muted");
        setContent(new VBox(6, welcome, role));
    }
}
