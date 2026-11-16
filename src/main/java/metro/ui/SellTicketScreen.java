package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.exception.MetroException;
import metro.model.Journey;
import metro.model.Leg;
import metro.model.Station;
import metro.model.Ticket;
import metro.model.TicketType;
import metro.service.TicketService;
import metro.util.Dates;
import metro.util.Money;

/**
 * Specification 3a and 3b: staff choose a journey, see the route and the
 * fare the system works out, and sell one or more tickets.
 */
public class SellTicketScreen extends Screen {

    private final TicketService service;
    private final StationPicker from;
    private final StationPicker to;
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final Spinner<Integer> quantity = new Spinner<>(1, TicketService.MAX_QUANTITY, 1);
    private final Button sell = new Button("Sell", Icons.of("confirmation_number", 16));
    private final VBox summary = new VBox(6);
    private final VBox soldCards = new VBox(12);
    private final VBox mySales = new VBox(4);

    public SellTicketScreen(AppContext context) {
        super(context, "Sell ticket");
        this.service = context.getTicketService();

        List<Station> open = new ArrayList<>();
        for (Station station : context.getStationService().listAll()) {
            if (station.isOpen()) {
                open.add(station);
            }
        }
        from = new StationPicker(open, "sell-from");
        to = new StationPicker(open, "sell-to");
        from.setOnChange(this::updateSummary);
        to.setOnChange(this::updateSummary);

        HBox types = new HBox();
        for (TicketType type : TicketType.values()) {
            ToggleButton button = new ToggleButton(type.getDisplayName());
            button.setUserData(type);
            button.setToggleGroup(typeGroup);
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            types.getChildren().add(button);
            if (type == TicketType.STANDARD) {
                button.setSelected(true);
            }
        }
        typeGroup.selectedToggleProperty().addListener((observable, before, after) -> {
            if (after == null) {
                typeGroup.selectToggle(before); // one type is always selected
            } else {
                updateSummary();
            }
        });
        quantity.valueProperty().addListener((observable, before, after) -> updateSummary());
        quantity.setMaxWidth(Double.MAX_VALUE);

        sell.setId("sell-button");
        sell.getStyleClass().add("accent");
        sell.setMaxWidth(Double.MAX_VALUE);
        sell.setOnAction(event -> run(this::sellTickets));

        VBox form = card("Journey",
                new Label("From"), from, new Label("To"), to,
                new Label("Ticket type"), types, new Label("Quantity"), quantity, sell);
        form.setMinWidth(280);
        form.setMaxWidth(300);

        VBox summaryCard = card("Route and fare", summary);
        summaryCard.setMinWidth(300);
        HBox.setHgrow(summaryCard, Priority.ALWAYS);

        ScrollPane cardsScroll = new ScrollPane(soldCards);
        cardsScroll.setFitToWidth(true);
        cardsScroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(cardsScroll, Priority.ALWAYS);
        VBox right = card("Tickets", cardsScroll, sectionTitle("My sales today"), mySales);
        right.setMinWidth(300);
        right.setMaxWidth(320);

        HBox columns = new HBox(18, form, summaryCard, right);
        setContent(columns);
        updateSummary();
    }

    @Override
    public void onShow() {
        showMySales();
    }

    private TicketType selectedType() {
        return (TicketType) typeGroup.getSelectedToggle().getUserData();
    }

    /** Sets both stations at once; used when the journey is picked on the map. */
    public void setJourney(Station origin, Station destination) {
        from.setValue(origin);
        to.setValue(destination);
        updateSummary();
    }

    /** Asks the service for the route and the fare of the current choice and shows them. */
    private void updateSummary() {
        summary.getChildren().clear();
        sell.setDisable(true);
        if (from.getValue() == null || to.getValue() == null) {
            summary.getChildren().add(muted("Choose where the passenger travels from and to."));
            return;
        }
        try {
            Journey journey = service.planJourney(from.getValue(), to.getValue());
            double each = service.quoteFare(journey, selectedType());
            List<Leg> legs = journey.getLegs();
            for (int i = 0; i < legs.size(); i++) {
                Leg leg = legs.get(i);
                if (i > 0) {
                    Label change = new Label("Change at " + leg.getFrom().getName(), Icons.of("swap_horiz", 16));
                    change.getStyleClass().add("text-muted");
                    summary.getChildren().add(change);
                }
                summary.getChildren().add(new LineSwatch(leg.getLine(), leg.getLine().getName() + ":  "
                        + leg.getFrom().getName() + " → " + leg.getTo().getName()
                        + "  (" + leg.getStops() + " stops)"));
            }
            summary.getChildren().add(spacer(10));
            summary.getChildren().add(row("Stops", String.valueOf(journey.getStops())));
            summary.getChildren().add(row("Changes", String.valueOf(journey.getTransfers())));
            summary.getChildren().add(row("Fare per ticket", Money.format(each)));
            Label total = new Label(Money.format(Money.round(each * quantity.getValue())));
            total.getStyleClass().add("big-number");
            total.setId("sell-total");
            summary.getChildren().addAll(spacer(6), muted("Total"), total);
            sell.setDisable(false);
        } catch (MetroException e) {
            Label reason = new Label(e.getMessage());
            reason.getStyleClass().add("form-error");
            reason.setWrapText(true);
            summary.getChildren().add(reason);
        }
    }

    private void sellTickets() {
        List<Ticket> sold = service.sell(from.getValue(), to.getValue(), selectedType(), quantity.getValue());
        soldCards.getChildren().clear();
        for (Ticket ticket : sold) {
            soldCards.getChildren().add(new TicketCard(ticket));
        }
        showMySales();
        showInfo(sold.size() == 1 ? "Ticket " + sold.get(0).getId() + " sold." : sold.size() + " tickets sold.");
    }

    private void showMySales() {
        mySales.getChildren().clear();
        List<Ticket> mine = service.mySalesToday();
        if (mine.isEmpty()) {
            mySales.getChildren().add(muted("No tickets sold yet today."));
        }
        for (int i = 0; i < mine.size() && i < 6; i++) {
            Ticket ticket = mine.get(i);
            Label line = new Label(ticket.getId() + "   " + Dates.time(ticket.getSoldAt()) + "   "
                    + Money.format(ticket.getFare()) + "   " + ticket.getStatus().getDisplayName());
            line.getStyleClass().add("text-small");
            mySales.getChildren().add(line);
        }
        if (mine.size() > 6) {
            mySales.getChildren().add(muted("and " + (mine.size() - 6) + " more"));
        }
    }

    // ----- small layout helpers -----

    private VBox card(String title, javafx.scene.Node... content) {
        VBox card = new VBox(10);
        card.getStyleClass().add("form-card");
        card.setPadding(new Insets(18));
        card.getChildren().add(sectionTitle(title));
        card.getChildren().addAll(content);
        return card;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-card-title");
        return label;
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-muted");
        label.setWrapText(true);
        return label;
    }

    private HBox row(String name, String value) {
        Label left = new Label(name);
        left.getStyleClass().add("text-muted");
        left.setMinWidth(130);
        return new HBox(8, left, new Label(value));
    }

    private javafx.scene.layout.Region spacer(double height) {
        javafx.scene.layout.Region region = new javafx.scene.layout.Region();
        region.setMinHeight(height);
        return region;
    }
}
