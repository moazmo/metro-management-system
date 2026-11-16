package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import metro.AppContext;
import metro.model.Permission;
import metro.model.User;

/**
 * The window a user sees after logging in: a top bar, the sidebar on the
 * left and the current screen in the middle.
 *
 * The list of sidebar entries is defined here, in one place. Each entry
 * names the permission it needs, so the menu a user sees depends on their role.
 */
public class MainWindow extends BorderPane {

    private final AppContext context;
    private final List<NavItem> items = new ArrayList<>();
    private final Sidebar sidebar;
    private final StackPane content = new StackPane();
    private final MenuButton userMenu = new MenuButton();
    private final Button themeButton = new Button();

    public MainWindow(AppContext context, Runnable onLogout) {
        this.context = context;
        getStyleClass().add("main-window");
        buildNavigation();

        sidebar = new Sidebar(items, context.getSession(), this::open);
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.getStyleClass().add("sidebar-scroll");

        setTop(buildTopBar(onLogout));
        setLeft(sidebarScroll);
        setCenter(content);
        open(getVisibleItems().get(0));
    }

    /** Every sidebar entry, in display order. */
    private void buildNavigation() {
        items.add(new NavItem(null, "Dashboard", "dashboard", null, () -> new DashboardScreen(context)));

        items.add(new NavItem("ADMINISTRATION", "Stations", "place", Permission.MANAGE_STATIONS,
                () -> new StationsScreen(context)));
        items.add(new NavItem("ADMINISTRATION", "Lines", "linear_scale", Permission.MANAGE_LINES,
                () -> new LinesScreen(context)));
        items.add(new NavItem("ADMINISTRATION", "Trains", "train", Permission.MANAGE_TRAINS,
                () -> new TrainsScreen(context)));

        items.add(new NavItem("SCHEDULE", "Trips", "schedule", Permission.VIEW_TRIPS,
                () -> new TripsScreen(context)));

        items.add(new NavItem("ACCOUNT", "Staff users", "people", Permission.MANAGE_USERS,
                () -> new StaffUsersScreen(context)));
        items.add(new NavItem("ACCOUNT", "My profile", "person", Permission.EDIT_OWN_PROFILE,
                () -> new ProfileScreen(context)));
    }

    private HBox buildTopBar(Runnable onLogout) {
        Label brand = new Label(MetroApp.TITLE, Icons.of("directions_subway", 22));
        brand.getStyleClass().add("brand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        themeButton.getStyleClass().addAll("flat", "button-icon");
        themeButton.setOnAction(event -> {
            Theme.toggle(getScene());
            refreshThemeButton();
        });
        refreshThemeButton();

        MenuItem logout = new MenuItem("Log out", Icons.of("exit_to_app", 16));
        logout.setOnAction(event -> onLogout.run());
        NavItem profile = findItem("My profile");
        if (profile != null) {
            MenuItem myProfile = new MenuItem("My profile", Icons.of("person", 16));
            myProfile.setOnAction(event -> open(profile));
            userMenu.getItems().add(myProfile);
        }
        userMenu.getItems().add(logout);
        userMenu.setGraphic(Icons.of("person", 18));
        userMenu.getStyleClass().add("flat");
        refreshUser();

        HBox bar = new HBox(12, brand, spacer, themeButton, userMenu);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private void refreshThemeButton() {
        themeButton.setGraphic(Icons.of(Theme.isDark() ? "brightness_7" : "brightness_4", 18));
    }

    /** Shows the logged-in user's name and role in the top bar. */
    public void refreshUser() {
        User user = context.getSession().getUser();
        userMenu.setText(user.getFullName() + "  ·  " + user.getRole().getDisplayName());
    }

    /** The sidebar entries the logged-in user is allowed to see. */
    public List<NavItem> getVisibleItems() {
        List<NavItem> visible = new ArrayList<>();
        for (NavItem item : items) {
            if (item.isVisibleTo(context.getSession())) {
                visible.add(item);
            }
        }
        return visible;
    }

    /** A visible entry by its label, or null when the user may not see it. */
    public NavItem findItem(String label) {
        for (NavItem item : getVisibleItems()) {
            if (item.getLabel().equals(label)) {
                return item;
            }
        }
        return null;
    }

    /** Opens the screen of a sidebar entry. */
    public void open(NavItem item) {
        Screen screen = item.createScreen();
        content.getChildren().setAll(screen);
        sidebar.select(item);
        refreshUser();
        screen.onShow();
    }
}
