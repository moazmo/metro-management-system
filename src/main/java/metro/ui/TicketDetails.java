package metro.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import metro.model.Ticket;
import metro.model.TicketStatus;
import metro.service.UserService;
import metro.util.Dates;
import metro.util.Money;

/**
 * A small table of everything known about one ticket. Used on the Validate
 * and Refunds screens.
 */
public class TicketDetails extends GridPane {

    private int nextRow = 0;

    public TicketDetails(Ticket ticket, UserService users) {
        setHgap(18);
        setVgap(6);
        add("Ticket", ticket.getId());
        add("Journey", ticket.getFrom().getName() + " → " + ticket.getTo().getName()
                + "  (" + ticket.getStops() + " stops)");
        add("Type", ticket.getType().getDisplayName());
        add("Fare", Money.format(ticket.getFare()));
        add("Sold", ticket.getSoldAt().format(Dates.DATE_TIME) + " by " + users.displayName(ticket.getSoldById()));
        add("Status", ticket.getStatus().getDisplayName());
        if (ticket.getStatus() == TicketStatus.USED) {
            add("Used", ticket.getUsedAt().format(Dates.DATE_TIME));
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            add("Refunded", ticket.getRefundedAt().format(Dates.DATE_TIME)
                    + " by " + users.displayName(ticket.getRefundedById()));
        }
    }

    private void add(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("text-muted");
        add(name, 0, nextRow);
        add(new Label(value), 1, nextRow);
        nextRow++;
    }
}
