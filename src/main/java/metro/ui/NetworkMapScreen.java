package metro.ui;

import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.exception.MetroException;
import metro.model.Journey;
import metro.model.Leg;
import metro.model.Station;
import metro.model.TicketType;
import metro.service.TicketService;
import metro.util.Money;

/**
 * The whole network on one screen. Click a station, then another: the
 * route between them lights up and the panel on the right lists its legs,
 * the stops, the changes and the fare of each ticket type.
 *
 * Every role that may view the network can use it, including roles that
 * cannot sell tickets: the route and the fares come from a preview that
 * sells nothing.
 */
public class NetworkMapScreen extends Screen {

    private final TicketService tickets;
    private final NetworkMapView map;
    private final VBox panel = new VBox(10);
    private Station from;
    private Station to;
    private Journey journey;

    public NetworkMapScreen(AppContext context) {
        super(context, "Network map");
        this.tickets = context.getTicketService();
        map = new NetworkMapView(context.getLineService().listAll());
        map.setOnStationClicked(this::choose);

        ScrollPane scroll = new ScrollPane(map);
        scroll.setPannable(true);
        scroll.getStyleClass().add("map-scroll");
        HBox.setHgrow(scroll, Priority.ALWAYS);

        panel.getStyleClass().add("form-card");
        panel.setPadding(new Insets(18));
        panel.setMinWidth(290);
        panel.setPrefWidth(290);
        panel.setMaxWidth(290);
        setContent(new HBox(16, scroll, panel));
        showPanel();
    }

    /**
     * What a click on a station does: the first click chooses where to
     * travel from, the second where to, and a third starts again.
     */
    public void choose(Station station) {
        if (from == null || to != null) {
            from = station;
            to = null;
            journey = null;
        } else if (!station.equals(from)) {
            to = station;
        }
        map.setSelection(from, to);
        showPanel();
    }

    private void clear() {
        from = null;
        to = null;
        journey = null;
        map.setSelection(null, null);
        showPanel();
    }

    /** The journey shown right now, or null. */
    public Journey getJourney() {
        return journey;
    }

    private void showPanel() {
        panel.getChildren().clear();
        Label title = new Label("Journey");
        title.getStyleClass().add("form-card-title");
        panel.getChildren().addAll(title,
                fact("From", from == null ? "click a station" : from.getName()),
                fact("To", to == null ? (from == null ? "" : "click another station") : to.getName()));

        journey = null;
        if (from != null && to != null) {
            try {
                journey = tickets.previewJourney(from, to);
                describe(journey);
            } catch (MetroException e) {
                Label reason = new Label(e.getMessage());
                reason.getStyleClass().add("form-error");
                reason.setWrapText(true);
                panel.getChildren().add(reason);
            }
        } else {
            Label hint = new Label("Click two stations on the map to see the route and the fare. "
                    + "Drag the map to move around it.");
            hint.getStyleClass().add("text-muted");
            hint.setWrapText(true);
            panel.getChildren().add(hint);
        }
        map.highlight(journey);

        Button clear = new Button("Clear", Icons.of("close", 16));
        clear.setOnAction(event -> clear());
        panel.getChildren().add(clear);
    }

    private void describe(Journey route) {
        VBox legs = new VBox(6);
        List<Leg> parts = route.getLegs();
        for (int i = 0; i < parts.size(); i++) {
            Leg leg = parts.get(i);
            if (i > 0) {
                Label change = new Label("Change at " + leg.getFrom().getName(), Icons.of("swap_horiz", 16));
                change.getStyleClass().add("text-muted");
                legs.getChildren().add(change);
            }
            LineSwatch swatch = new LineSwatch(leg.getLine(), leg.getLine().getName() + ": "
                    + leg.getFrom().getName() + " → " + leg.getTo().getName() + " (" + leg.getStops() + ")");
            legs.getChildren().add(swatch);
        }
        panel.getChildren().addAll(legs,
                fact("Stops", String.valueOf(route.getStops())),
                fact("Changes", String.valueOf(route.getTransfers())));
        Label fares = new Label("Fares");
        fares.getStyleClass().add("form-card-title");
        panel.getChildren().add(fares);
        for (TicketType type : TicketType.values()) {
            panel.getChildren().add(fact(type.getDisplayName(), Money.format(tickets.quoteFare(route, type))));
        }
    }

    private HBox fact(String name, String value) {
        Label left = new Label(name);
        left.getStyleClass().add("text-muted");
        left.setMinWidth(90);
        Label right = new Label(value);
        right.setWrapText(true);
        return new HBox(8, left, right);
    }
}
