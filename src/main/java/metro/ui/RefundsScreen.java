package metro.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.model.Ticket;
import metro.model.TicketCheck;
import metro.service.TicketService;
import metro.util.Money;

/**
 * Specification 3d: authorized staff find a ticket, see whether it may be
 * refunded and why, and record the refund.
 */
public class RefundsScreen extends Screen {

    private final TicketService service;
    private final TextField code = new TextField();
    private final VBox result = new VBox(12);

    public RefundsScreen(AppContext context) {
        super(context, "Refunds");
        this.service = context.getTicketService();

        code.setId("refund-code");
        code.setPromptText("Ticket code, for example TKT-000123");
        code.getStyleClass().add("code-field");
        code.setPrefWidth(360);
        code.setOnAction(event -> run(this::find));
        Button find = new Button("Find", Icons.of("search", 16));
        find.setId("refund-find");
        find.setOnAction(event -> run(this::find));

        HBox entry = new HBox(10, code, find);
        entry.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("A ticket can be refunded only if it was never used and was sold today.");
        hint.getStyleClass().add("text-muted");

        setContent(new VBox(14, entry, hint, result));
    }

    /** Looks the ticket up and shows whether a refund is allowed. */
    private void find() {
        result.getChildren().clear();
        TicketCheck check = service.refundCheck(code.getText());
        Label message = new Label(check.getMessage(),
                Icons.of(check.isAccepted() ? "check_circle" : "info", 28));
        message.getStyleClass().add("verdict-text");
        VBox panel = new VBox(14, message);
        panel.getStyleClass().addAll("verdict", check.isAccepted() ? "verdict-accepted" : "verdict-neutral");
        panel.setMaxWidth(640);

        Ticket ticket = check.getTicket();
        if (ticket != null) {
            panel.getChildren().add(new TicketDetails(ticket, context.getUserService()));
            Button refund = new Button("Refund " + Money.format(ticket.getFare()), Icons.of("undo", 16));
            refund.setId("refund-button");
            refund.getStyleClass().add("accent");
            refund.setDisable(!check.isAccepted());
            refund.setOnAction(event -> run(() -> {
                if (confirm("Refund " + Money.format(ticket.getFare()) + " for ticket " + ticket.getId() + "?")) {
                    service.refund(ticket.getId());
                    find();
                    showInfo("Refund recorded for " + ticket.getId() + ".");
                }
            }));
            panel.getChildren().add(refund);
        }
        result.getChildren().add(panel);
    }
}
