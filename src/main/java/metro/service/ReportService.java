package metro.service;

import java.time.LocalDate;
import metro.model.Line;
import metro.model.Permission;
import metro.model.TripStatus;
import metro.repository.AuditLog;
import metro.repository.TicketRepository;
import metro.repository.TripRepository;
import metro.service.report.Report;
import metro.service.report.RevenueReport;
import metro.service.report.TicketReport;
import metro.service.report.TripReport;
import metro.util.Dates;

/**
 * Creates the reports of specification module 4. Only users who may view
 * reports (admins) can call these methods.
 */
public class ReportService {

    private final TripRepository trips;
    private final TicketRepository tickets;
    private final Session session;
    private final AuditLog audit;

    public ReportService(TripRepository trips, TicketRepository tickets, Session session, AuditLog audit) {
        this.trips = trips;
        this.tickets = tickets;
        this.session = session;
        this.audit = audit;
    }

    /** Trips by date, and optionally by line and by status (4a). */
    public TripReport tripReport(LocalDate from, LocalDate to, Line line, TripStatus status) {
        session.require(Permission.VIEW_REPORTS);
        TripReport report = new TripReport(trips.findAll(), from, to, line, status);
        record(report);
        return report;
    }

    /** Tickets sold for each journey (4b). */
    public TicketReport ticketReport(LocalDate from, LocalDate to) {
        session.require(Permission.VIEW_REPORTS);
        TicketReport report = new TicketReport(tickets.findAll(), from, to);
        record(report);
        return report;
    }

    /** Ticket revenue for a period (4c). */
    public RevenueReport revenueReport(LocalDate from, LocalDate to) {
        session.require(Permission.VIEW_REPORTS);
        RevenueReport report = new RevenueReport(tickets.findAll(), from, to);
        record(report);
        return report;
    }

    /**
     * Notes in the audit log that a report was generated. The parameter is
     * the abstract type Report, so this one method serves all three kinds.
     */
    private void record(Report report) {
        audit.record(session.getUser().getUsername(), "REPORT_GENERATED", report.getTitle() + " "
                + report.getFrom().format(Dates.ISO_DATE) + " to " + report.getTo().format(Dates.ISO_DATE));
    }
}
