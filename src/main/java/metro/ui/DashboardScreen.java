package metro.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import metro.AppContext;
import metro.model.Permission;
import metro.model.Trip;
import metro.model.User;
import metro.service.DashboardService;
import metro.service.Session;
import metro.util.Dates;
import metro.util.Money;

/**
 * The screen every user lands on after logging in: a greeting, live figures
 * for today, a small network map and the next departures.
 *
 * Each figure is shown only to roles that may see the information behind it,
 * so an agent never sees revenue and a controller never sees sales.
 */
public class DashboardScreen extends Screen {

    private final DashboardService service;
    private final Session session;
    private final VBox body = new VBox(18);

    public DashboardScreen(AppContext context) {
        super(context, "Dashboard");
        this.service = context.getDashboardService();
        this.session = context.getSession();
        setContent(body);
    }

    /** The figures are read again every time the dashboard is opened. */
    @Override
    public void onShow() {
        body.getChildren().clear();
        User user = session.getUser();
        Label greeting = new Label(greeting() + ", " + user.getFullName());
        greeting.getStyleClass().add("title-3");
        Label role = new Label(user.getRole().getDisplayName() + "  ·  "
                + Dates.date(LocalDate.now(context.getClock())));
        role.getStyleClass().add("text-muted");
        body.getChildren().add(new VBox(4, greeting, role));

        HBox tiles = new HBox(14);
        if (session.can(Permission.VIEW_TRIPS)) {
            tiles.getChildren().add(new KpiTile("Trips today", String.valueOf(service.tripsToday()), "schedule"));
            tiles.getChildren().add(new KpiTile("Delayed today", String.valueOf(service.delayedToday()), "more_time"));
        }
        if (session.can(Permission.VIEW_REPORTS)) {
            tiles.getChildren().add(new KpiTile("Tickets sold today",
                    String.valueOf(service.ticketsSoldToday()), "confirmation_number"));
            tiles.getChildren().add(new KpiTile("Revenue today", Money.format(service.revenueToday()), "payments"));
        } else if (session.can(Permission.SELL_TICKETS)) {
            tiles.getChildren().add(new KpiTile("My sales today",
                    String.valueOf(service.mySalesToday()), "confirmation_number"));
        }
        body.getChildren().add(tiles);

        HBox panels = new HBox(18);
        VBox.setVgrow(panels, Priority.ALWAYS);
        if (session.can(Permission.VIEW_NETWORK)) {
            VBox mapPanel = panel("Network", miniMap());
            HBox.setHgrow(mapPanel, Priority.ALWAYS);
            panels.getChildren().add(mapPanel);
        }
        if (session.can(Permission.VIEW_TRIPS)) {
            VBox departures = panel("Next departures today", departures());
            departures.setMinWidth(360);
            departures.setPrefWidth(380);
            panels.getChildren().add(departures);
        }
        body.getChildren().add(panels);
    }

    private String greeting() {
        int hour = LocalDateTime.now(context.getClock()).getHour();
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 18 ? "Good afternoon" : "Good evening";
    }

    /** The network map, shrunk to fit its panel. Clicking it opens the full map. */
    private StackPane miniMap() {
        NetworkMapView view = new NetworkMapView(context.getLineService().listAll());
        view.setMouseTransparent(true);
        Scale scale = new Scale(0.5, 0.5, 0, 0);
        view.getTransforms().add(scale);
        StackPane holder = new StackPane(new Group(view));
        holder.setAlignment(Pos.TOP_LEFT);
        holder.setMinHeight(200);
        holder.widthProperty().addListener((observable, before, width) -> {
            double factor = Math.min(1.0, (width.doubleValue() - 8) / view.getPrefWidth());
            scale.setX(factor);
            scale.setY(factor);
        });
        holder.getStyleClass().add("mini-map");
        holder.setOnMouseClicked(event -> openScreen("Network map"));
        return holder;
    }

    private VBox departures() {
        VBox list = new VBox(8);
        LocalDate today = LocalDate.now(context.getClock());
        int shown = 0;
        for (Trip trip : service.nextDepartures(8)) {
            if (!trip.getDeparture().toLocalDate().equals(today)) {
                continue;
            }
            Label time = new Label(Dates.time(trip.getDeparture()));
            time.getStyleClass().add("departure-time");
            time.setMinWidth(44);
            LineSwatch route = new LineSwatch(trip.getLine(), trip.getOrigin().getName() + " → "
                    + trip.getDestination().getName());
            HBox.setHgrow(route, Priority.ALWAYS);
            route.setMinWidth(0);
            HBox row = new HBox(10, time, route, TripsScreen.badge(trip.getStatus()));
            row.setAlignment(Pos.CENTER_LEFT);
            list.getChildren().add(row);
            shown++;
        }
        if (shown == 0) {
            Label none = new Label("No more departures today.");
            none.getStyleClass().add("text-muted");
            list.getChildren().add(none);
        }
        return list;
    }

    private VBox panel(String title, javafx.scene.Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("form-card-title");
        VBox panel = new VBox(12, heading, content);
        panel.getStyleClass().add("form-card");
        panel.setPadding(new Insets(18));
        return panel;
    }

    /** Opens another screen of the main window by its sidebar label. */
    private void openScreen(String label) {
        if (getScene() != null && getScene().getRoot() instanceof MainWindow) {
            MainWindow window = (MainWindow) getScene().getRoot();
            NavItem item = window.findItem(label);
            if (item != null) {
                window.open(item);
            }
        }
    }
}
