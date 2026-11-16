package metro.service.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import metro.model.Ticket;
import metro.model.TicketStatus;
import metro.util.Dates;
import metro.util.Money;

/**
 * Specification 4c: recorded ticket revenue for a selected period.
 *
 * A sale counts on the day the ticket was sold, at its fare, whether or not
 * it was refunded later. A refund counts on the day it was recorded.
 * Net revenue is sales minus refunds. Every day of the period is listed,
 * including days on which nothing happened.
 */
public class RevenueReport extends Report {

    private final List<DayRevenue> days = new ArrayList<>();

    public RevenueReport(List<Ticket> allTickets, LocalDate from, LocalDate to) {
        super("Revenue report", from, to);
        LocalDate day = from;
        while (!day.isAfter(to)) {
            days.add(new DayRevenue(day));
            day = day.plusDays(1);
        }
        for (Ticket ticket : allTickets) {
            if (inRange(ticket.getSoldAt())) {
                dayOf(ticket.getSoldAt().toLocalDate()).addSale(ticket.getFare());
            }
            if (ticket.getStatus() == TicketStatus.REFUNDED && inRange(ticket.getRefundedAt())) {
                dayOf(ticket.getRefundedAt().toLocalDate()).addRefund(ticket.getFare());
            }
        }
    }

    private DayRevenue dayOf(LocalDate date) {
        for (DayRevenue day : days) {
            if (day.getDate().equals(date)) {
                return day;
            }
        }
        throw new IllegalStateException("No row for " + date);
    }

    public List<DayRevenue> getDays() {
        return new ArrayList<>(days);
    }

    public int getTotalTicketsSold() {
        int total = 0;
        for (DayRevenue day : days) {
            total += day.getTicketsSold();
        }
        return total;
    }

    public double getTotalSales() {
        double total = 0;
        for (DayRevenue day : days) {
            total += day.getSales();
        }
        return Money.round(total);
    }

    public double getTotalRefunds() {
        double total = 0;
        for (DayRevenue day : days) {
            total += day.getRefunds();
        }
        return Money.round(total);
    }

    public double getNet() {
        return Money.round(getTotalSales() - getTotalRefunds());
    }

    @Override
    public List<String> getColumns() {
        return Arrays.asList("Date", "Tickets sold", "Sales", "Refunds", "Net");
    }

    /** One row per day, and a last row with the totals. */
    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (DayRevenue day : days) {
            rows.add(new String[] {
                day.getDate().format(Dates.ISO_DATE),
                String.valueOf(day.getTicketsSold()),
                Money.plain(day.getSales()),
                Money.plain(day.getRefunds()),
                Money.plain(day.getNet())
            });
        }
        rows.add(new String[] {
            "Total",
            String.valueOf(getTotalTicketsSold()),
            Money.plain(getTotalSales()),
            Money.plain(getTotalRefunds()),
            Money.plain(getNet())
        });
        return rows;
    }
}
