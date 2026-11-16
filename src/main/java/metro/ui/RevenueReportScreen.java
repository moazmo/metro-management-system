package metro.ui;

import java.time.LocalDate;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import metro.AppContext;
import metro.service.report.DayRevenue;
import metro.service.report.Report;
import metro.service.report.RevenueReport;
import metro.util.Dates;
import metro.util.Money;

/**
 * Specification 4c: ticket revenue for a selected period, with the totals
 * in large figures and a chart of net revenue per day.
 */
public class RevenueReportScreen extends ReportScreen {

    public RevenueReportScreen(AppContext context) {
        super(context, "Revenue report");
    }

    @Override
    protected Report generate(LocalDate from, LocalDate to) {
        return context.getReportService().revenueReport(from, to);
    }

    /** The revenue report always lists every day, so "no data" means nothing was sold or refunded. */
    @Override
    protected boolean hasData(Report report) {
        RevenueReport revenue = (RevenueReport) report;
        return revenue.getTotalTicketsSold() > 0 || revenue.getTotalRefunds() > 0;
    }

    @Override
    protected Node buildSummary(Report report) {
        RevenueReport revenue = (RevenueReport) report;
        return row(
                counter("Sales", Money.format(revenue.getTotalSales())),
                counter("Refunds", Money.format(revenue.getTotalRefunds())),
                counter("Net revenue", Money.format(revenue.getNet())),
                counter("Tickets sold", String.valueOf(revenue.getTotalTicketsSold())));
    }

    @Override
    protected Node buildChart(Report report) {
        CategoryAxis days = new CategoryAxis();
        NumberAxis amounts = new NumberAxis();
        amounts.setLabel("EGP");
        amounts.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(days, amounts);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DayRevenue day : ((RevenueReport) report).getDays()) {
            series.getData().add(new XYChart.Data<>(day.getDate().format(Dates.DAY_MONTH), day.getNet()));
        }
        chart.getData().add(series);
        chart.setTitle("Net revenue per day");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        return chart;
    }
}
