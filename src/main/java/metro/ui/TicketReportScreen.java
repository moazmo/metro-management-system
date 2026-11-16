package metro.ui;

import java.time.LocalDate;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import metro.AppContext;
import metro.service.report.JourneyCount;
import metro.service.report.Report;
import metro.service.report.TicketReport;

/**
 * Specification 4b: the number of tickets sold for each journey, with a
 * chart of the ten busiest journeys.
 */
public class TicketReportScreen extends ReportScreen {

    public TicketReportScreen(AppContext context) {
        super(context, "Ticket report");
    }

    @Override
    protected Report generate(LocalDate from, LocalDate to) {
        return context.getReportService().ticketReport(from, to);
    }

    @Override
    protected Node buildSummary(Report report) {
        TicketReport tickets = (TicketReport) report;
        return row(
                counter("Tickets sold (not refunded)", String.valueOf(tickets.getTotalTickets())),
                counter("Different journeys", String.valueOf(tickets.getJourneys().size())));
    }

    @Override
    protected Node buildChart(Report report) {
        List<JourneyCount> journeys = ((TicketReport) report).getJourneys();
        CategoryAxis names = new CategoryAxis();
        int shown = Math.min(8, journeys.size());
        int most = shown == 0 ? 1 : journeys.get(0).getTickets();
        // Ticket counts are whole numbers, so the axis counts in steps of one.
        NumberAxis counts = new NumberAxis(0, most + 1, 1);
        counts.setMinorTickVisible(false);
        BarChart<Number, String> chart = new BarChart<>(counts, names);
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        // The busiest journey is added last so that it is drawn at the top.
        for (int i = shown - 1; i >= 0; i--) {
            JourneyCount journey = journeys.get(i);
            series.getData().add(new XYChart.Data<>(journey.getTickets(),
                    journey.getFrom().getName() + " → " + journey.getTo().getName()));
        }
        chart.getData().add(series);
        chart.setTitle("Busiest journeys (tickets sold)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(4);
        chart.getStyleClass().add("journey-chart");
        return chart;
    }
}
