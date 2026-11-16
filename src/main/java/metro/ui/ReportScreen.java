package metro.ui;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import metro.AppContext;
import metro.service.report.Report;
import metro.util.Dates;

/**
 * Base class of the three report screens.
 *
 * What they share lives here: the date range, the Generate and Export
 * buttons, the table and the empty-state message. The table is filled from
 * Report.getColumns() and Report.getRows(), so it works for any report.
 * A subclass says how to create its report and which chart and figures to
 * show above the table.
 */
public abstract class ReportScreen extends Screen {

    private static final String NO_DATA = "No data for these dates.";

    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final HBox filters = new HBox(10);
    private final VBox summary = new VBox();
    private final StackPane chartArea = new StackPane();
    private final TableView<String[]> table = new TableView<>();
    private Report current;

    protected ReportScreen(AppContext context, String title) {
        super(context, title);
        LocalDate today = LocalDate.now(context.getClock());
        from.setValue(today.minusDays(6));
        to.setValue(today);
        from.setId("report-from");
        to.setId("report-to");
        from.setPrefWidth(140);
        to.setPrefWidth(140);

        Button generate = new Button("Generate", Icons.of("refresh", 16));
        generate.setId("report-generate");
        generate.getStyleClass().add("accent");
        generate.setOnAction(event -> run(this::generateNow));
        Button export = new Button("Export CSV", Icons.of("save_alt", 16));
        export.setId("report-export");
        export.setOnAction(event -> run(this::chooseFileAndExport));

        filters.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, new Label("From"), from, new Label("To"), to, filters, spacer, generate, export);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        chartArea.getStyleClass().add("chart-area");
        chartArea.setMinHeight(250);
        chartArea.setPrefHeight(270);
        table.setId("report-table");
        table.setPlaceholder(new Label(NO_DATA));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        setContent(new VBox(14, toolbar, summary, chartArea, table));
    }

    /** Creates the report for the chosen dates. Extra filters are read by the subclass. */
    protected abstract Report generate(LocalDate fromDate, LocalDate toDate);

    /** The chart shown above the table, for a report that has data. */
    protected abstract Node buildChart(Report report);

    /** Small figures shown above the chart, or null for none. */
    protected Node buildSummary(Report report) {
        return null;
    }

    /** Places an extra filter beside the date range. */
    protected final void addFilter(String label, Node control) {
        filters.getChildren().addAll(new Label(label), control);
    }

    /** The report is generated once when the screen opens. */
    @Override
    public void onShow() {
        run(this::generateNow);
    }

    private void generateNow() {
        Report report = generate(from.getValue(), to.getValue());
        current = report;

        table.getColumns().clear();
        List<String> columns = report.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            final int index = i;
            TableColumn<String[], String> column = new TableColumn<>(columns.get(i));
            column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue()[index]));
            table.getColumns().add(column);
        }
        table.getItems().setAll(report.getRows());

        summary.getChildren().clear();
        Node figures = buildSummary(report);
        if (figures != null) {
            summary.getChildren().add(figures);
        }
        chartArea.getChildren().clear();
        if (hasData(report)) {
            chartArea.getChildren().add(buildChart(report));
        } else {
            Label empty = new Label(NO_DATA);
            empty.getStyleClass().add("text-muted");
            chartArea.getChildren().add(empty);
        }
    }

    /** Whether the report found anything; the revenue report always has rows, so it decides for itself. */
    protected boolean hasData(Report report) {
        return !report.getRows().isEmpty();
    }

    private void chooseFileAndExport() {
        if (current == null) {
            generateNow();
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + current.getTitle());
        chooser.setInitialFileName(defaultFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            exportTo(file.toPath());
        }
    }

    /** Writes the report currently shown to a CSV file. */
    public void exportTo(Path file) {
        if (current == null) {
            generateNow();
        }
        current.exportCsv(file);
        showInfo("Saved " + file.getFileName());
    }

    /** For example "revenue-report-2026-11-09-to-2026-11-15.csv". */
    public String defaultFileName() {
        return getTitle().toLowerCase().replace(' ', '-') + "-" + from.getValue().format(Dates.ISO_DATE)
                + "-to-" + to.getValue().format(Dates.ISO_DATE) + ".csv";
    }

    // ----- helpers for the subclasses -----

    /** A small box with a number and a label under it. */
    protected static VBox counter(String label, String value) {
        Label number = new Label(value);
        number.getStyleClass().add("counter-value");
        Label caption = new Label(label);
        caption.getStyleClass().add("text-muted");
        VBox box = new VBox(2, number, caption);
        box.getStyleClass().add("counter");
        return box;
    }

    protected static HBox row(Node... counters) {
        return new HBox(14, counters);
    }
}
