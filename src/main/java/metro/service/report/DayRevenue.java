package metro.service.report;

import java.time.LocalDate;
import metro.util.Money;

/** One row of the revenue report: the money taken and given back on one day. */
public class DayRevenue {

    private final LocalDate date;
    private int ticketsSold;
    private double sales;
    private int refundCount;
    private double refunds;

    DayRevenue(LocalDate date) {
        this.date = date;
    }

    /** Only the revenue report calls these two. */
    void addSale(double fare) {
        ticketsSold++;
        sales = Money.round(sales + fare);
    }

    void addRefund(double fare) {
        refundCount++;
        refunds = Money.round(refunds + fare);
    }

    public LocalDate getDate() {
        return date;
    }

    public int getTicketsSold() {
        return ticketsSold;
    }

    public double getSales() {
        return sales;
    }

    public int getRefundCount() {
        return refundCount;
    }

    public double getRefunds() {
        return refunds;
    }

    /** What is left of the day's sales after the day's refunds. */
    public double getNet() {
        return Money.round(sales - refunds);
    }
}
