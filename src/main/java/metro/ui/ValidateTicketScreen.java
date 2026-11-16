package metro.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.model.TicketCheck;
import metro.service.TicketService;

/**
 * Specification 3c: staff enter a ticket code, the system checks the ticket
 * and, when it is acceptable, marks it as used.
 */
public class ValidateTicketScreen extends Screen {

    private final TicketService service;
    private final TextField code = new TextField();
    private final VBox verdict = new VBox(12);

    public ValidateTicketScreen(AppContext context) {
        super(context, "Validate ticket");
        this.service = context.getTicketService();

        code.setId("validate-code");
        code.setPromptText("Ticket code, for example TKT-000123");
        code.getStyleClass().add("code-field");
        code.setPrefWidth(360);
        code.setOnAction(event -> run(() -> show(service.inspect(code.getText()))));

        Button check = new Button("Check", Icons.of("search", 16));
        check.setId("validate-check");
        check.setOnAction(event -> run(() -> show(service.inspect(code.getText()))));
        Button validate = new Button("Validate", Icons.of("fact_check", 16));
        validate.setId("validate-button");
        validate.getStyleClass().add("accent");
        validate.setOnAction(event -> run(() -> {
            show(service.validate(code.getText()));
            code.clear();
            code.requestFocus();
        }));

        HBox entry = new HBox(10, code, check, validate);
        entry.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("Check only looks at the ticket. Validate also marks an acceptable ticket as used.");
        hint.getStyleClass().add("text-muted");

        setContent(new VBox(14, entry, hint, verdict));
    }

    /** Shows the answer in green or red, with the ticket's details when the ticket exists. */
    private void show(TicketCheck result) {
        verdict.getChildren().clear();
        Label message = new Label(result.getMessage(),
                Icons.of(result.isAccepted() ? "check_circle" : "cancel", 32));
        message.getStyleClass().add("verdict-text");
        VBox panel = new VBox(14, message);
        panel.getStyleClass().addAll("verdict", result.isAccepted() ? "verdict-accepted" : "verdict-rejected");
        if (result.getTicket() != null) {
            panel.getChildren().add(new TicketDetails(result.getTicket(), context.getUserService()));
        }
        panel.setMaxWidth(640);
        verdict.getChildren().add(panel);
    }
}
