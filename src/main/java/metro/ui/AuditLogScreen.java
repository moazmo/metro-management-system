package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.model.AuditEntry;
import metro.model.Permission;
import metro.util.Dates;

/**
 * Admins read the audit log here: who did what and when, newest first,
 * with a search box that matches any column.
 */
public class AuditLogScreen extends Screen {

    private final TextField search = new TextField();
    private final Label count = new Label();
    private final TableView<AuditEntry> table = new TableView<>();
    private List<AuditEntry> all = new ArrayList<>();

    public AuditLogScreen(AppContext context) {
        super(context, "Audit log");
        search.setPromptText("Search…");
        search.setId("audit-search");
        search.setMaxWidth(320);
        search.textProperty().addListener((observable, before, after) -> filter());
        count.getStyleClass().add("text-muted");
        table.setId("audit-table");

        column("Time", entry -> entry.getTime().format(Dates.ISO_DATE_TIME));
        column("User", entry -> entry.getUsername());
        column("Action", entry -> entry.getAction());
        column("Detail", entry -> entry.getDetail());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No entries match."));
        VBox.setVgrow(table, Priority.ALWAYS);
        setContent(new VBox(12, search, table, count));
    }

    private void column(String heading, java.util.function.Function<AuditEntry, String> text) {
        TableColumn<AuditEntry, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new SimpleStringProperty(text.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    /** The log is read from the file every time the screen opens. */
    @Override
    public void onShow() {
        run(() -> {
            context.getSession().require(Permission.VIEW_AUDIT_LOG);
            all = context.getAuditLog().readAll();
            filter();
        });
    }

    private void filter() {
        String wanted = search.getText().trim().toLowerCase();
        List<AuditEntry> shown = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            AuditEntry entry = all.get(i);
            String line = (entry.getTime().format(Dates.ISO_DATE_TIME) + " " + entry.getUsername() + " "
                    + entry.getAction() + " " + entry.getDetail()).toLowerCase();
            if (wanted.isEmpty() || line.contains(wanted)) {
                shown.add(entry);
            }
        }
        table.getItems().setAll(shown);
        count.setText("Showing " + shown.size() + " of " + all.size());
    }
}
