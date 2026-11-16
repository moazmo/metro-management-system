package metro.ui;

import java.time.LocalDate;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import metro.AppContext;
import metro.model.Line;
import metro.model.TripStatus;
import metro.service.report.Report;
import metro.service.report.TripReport;

/**
 * Specification 4a: trips by date, line and service status, with a chart of
 * how many trips ended in each status.
 */
public class TripReportScreen extends ReportScreen {

    private static final String ALL_LINES = "All lines";
    private static final String ALL_STATUSES = "All statuses";

    private final ComboBox<String> lineFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public TripReportScreen(AppContext context) {
        super(context, "Trip report");
        lineFilter.getItems().add(ALL_LINES);
        for (Line line : context.getLineService().listAll()) {
            lineFilter.getItems().add(line.getName());
        }
        lineFilter.setValue(ALL_LINES);
        lineFilter.setPrefWidth(150);
        statusFilter.setId("report-status");
        statusFilter.getItems().add(ALL_STATUSES);
        for (TripStatus status : TripStatus.values()) {
            statusFilter.getItems().add(status.getDisplayName());
        }
        statusFilter.setValue(ALL_STATUSES);
        addFilter("Line", lineFilter);
        addFilter("Status", statusFilter);
    }

    @Override
    protected Report generate(LocalDate from, LocalDate to) {
        Line line = null;
        for (Line candidate : context.getLineService().listAll()) {
            if (candidate.getName().equals(lineFilter.getValue())) {
                line = candidate;
            }
        }
        TripStatus status = null;
        for (TripStatus candidate : TripStatus.values()) {
            if (candidate.getDisplayName().equals(statusFilter.getValue())) {
                status = candidate;
            }
        }
        return context.getReportService().tripReport(from, to, line, status);
    }

    @Override
    protected Node buildSummary(Report report) {
        TripReport trips = (TripReport) report;
        return row(
                counter("Scheduled", String.valueOf(trips.count(TripStatus.SCHEDULED))),
                counter("Delayed", String.valueOf(trips.count(TripStatus.DELAYED))),
                counter("Completed", String.valueOf(trips.count(TripStatus.COMPLETED))),
                counter("Cancelled", String.valueOf(trips.count(TripStatus.CANCELLED))));
    }

    @Override
    protected Node buildChart(Report report) {
        TripReport trips = (TripReport) report;
        PieChart chart = new PieChart();
        for (TripStatus status : TripStatus.values()) {
            int count = trips.count(status);
            if (count > 0) {
                PieChart.Data slice = new PieChart.Data(status.getDisplayName() + " (" + count + ")", count);
                chart.getData().add(slice);
                // the same colours as the status badges in the Trips table
                slice.getNode().setStyle("-fx-pie-color: " + colourOf(status) + ";");
            }
        }
        chart.setTitle("Trips by status");
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);
        return chart;
    }

    private static String colourOf(TripStatus status) {
        switch (status) {
            case DELAYED:
                return "#D4A72C";
            case COMPLETED:
                return "#2DA44E";
            case CANCELLED:
                return "#CF222E";
            default:
                return "#8C959F";
        }
    }
}
