package metro.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import metro.model.Ticket;
import metro.util.Dates;
import metro.util.Money;

/**
 * Shows a ticket the way a printed ticket would look: the code in large
 * letters, the journey, the type, the fare and the day it is valid on.
 */
public class TicketCard extends VBox {

    public TicketCard(Ticket ticket) {
        getStyleClass().add("ticket-card");
        setSpacing(8);

        Label brand = new Label("METRO TICKET", Icons.of("directions_subway", 16));
        brand.getStyleClass().add("ticket-brand");
        Label type = new Label(ticket.getType().getDisplayName().toUpperCase());
        type.getStyleClass().add("ticket-type");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, brand, spacer, type);

        Label code = new Label(ticket.getId());
        code.getStyleClass().add("ticket-code");

        Label journey = new Label(ticket.getFrom().getName() + "  →  " + ticket.getTo().getName());
        journey.getStyleClass().add("ticket-journey");
        journey.setWrapText(true);
        Label stops = new Label(ticket.getStops() + " stops");
        stops.getStyleClass().add("text-muted");

        Label fare = new Label(Money.format(ticket.getFare()));
        fare.getStyleClass().add("ticket-fare");
        Label sold = new Label("Sold " + Dates.date(ticket.getSoldAt().toLocalDate())
                + " at " + Dates.time(ticket.getSoldAt()));
        sold.getStyleClass().add("text-muted");
        Label valid = new Label("Valid on " + Dates.date(ticket.getSoldAt().toLocalDate()) + " only");
        valid.getStyleClass().add("text-small");

        getChildren().addAll(top, code, new Separator(), journey, stops, fare, sold, valid);
    }
}
