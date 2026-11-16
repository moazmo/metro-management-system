package metro.service.report;

import static metro.TestData.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import metro.TestData;
import metro.exception.AccessDeniedException;
import metro.exception.ValidationException;
import metro.model.Direction;
import metro.model.Line;
import metro.model.Station;
import metro.model.Ticket;
import metro.model.TicketFactory;
import metro.model.TicketType;
import metro.model.Train;
import metro.model.Trip;
import metro.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Trips:   Red 14th 09:00 Completed · Red 15th 09:00 Scheduled · Blue 15th 23:30 Delayed · Blue 16th 00:10 Cancelled
 * Tickets: 13th: A to G Standard 8.00
 *          15th: A to G Standard 8.00 x3 (one refunded the same day) · A to E Student 4.00 · G to A Standard 8.00 at 23:59
 */
class ReportsTest {

    static final LocalDate D13 = LocalDate.of(2026, 11, 13);
    static final LocalDate D14 = LocalDate.of(2026, 11, 14);
    static final LocalDate D15 = LocalDate.of(2026, 11, 15);
    static final LocalDate D16 = LocalDate.of(2026, 11, 16);

    @TempDir
    Path dir;
    TestData data;
    Trip lateBlue;
    Trip cancelledBlue;

    @BeforeEach
    void setUp() {
        data = new TestData(dir);
        data.loginAs(data.admin);

        trip(data.train1, data.red, on(14, 9, 0), TripStatus.COMPLETED, 0);
        trip(data.train1, data.red, on(15, 9, 0), TripStatus.SCHEDULED, 0);
        lateBlue = trip(data.train2, data.blue, on(15, 23, 30), TripStatus.DELAYED, 10);
        cancelledBlue = trip(data.train1, data.blue, on(16, 0, 10), TripStatus.CANCELLED, 0);

        ticket(TicketType.STANDARD, data.stationA, data.stationG, 3, on(13, 12, 0));
        ticket(TicketType.STANDARD, data.stationA, data.stationG, 3, on(15, 8, 0));
        ticket(TicketType.STANDARD, data.stationA, data.stationG, 3, on(15, 8, 5));
        Ticket refunded = ticket(TicketType.STANDARD, data.stationA, data.stationG, 3, on(15, 8, 10));
        refunded.refund(data.supervisor.getId(), on(15, 8, 30));
        data.tickets.save(refunded);
        ticket(TicketType.STUDENT, data.stationA, data.stationE, 4, on(15, 9, 0));
        ticket(TicketType.STANDARD, data.stationG, data.stationA, 3, on(15, 23, 59));
    }

    private Trip trip(Train train, Line line, LocalDateTime departure, TripStatus status, int delay) {
        Trip trip = new Trip(data.trips.nextId(), train, line, Direction.FORWARD,
                departure, departure.plusMinutes(30), status, delay);
        data.trips.save(trip);
        return trip;
    }

    private Ticket ticket(TicketType type, Station from, Station to, int stops, LocalDateTime soldAt) {
        Ticket ticket = TicketFactory.create(type, data.tickets.nextId(), from, to, stops, 8.00,
                data.agent.getId(), soldAt);
        data.tickets.save(ticket);
        return ticket;
    }

    // ----- trip report (4a) -----

    @Test
    void tripRangeIncludesBothEnds() {
        assertEquals(3, data.reportService.tripReport(D14, D15, null, null).getTrips().size());
    }

    @Test
    void lateEveningTripBelongsToItsDepartureDay() {
        List<Trip> trips = data.reportService.tripReport(D15, D15, null, null).getTrips();
        assertTrue(trips.contains(lateBlue));
        assertFalse(trips.contains(cancelledBlue));
        assertEquals(2, trips.size());
    }

    @Test
    void tripFiltersCombine() {
        TripReport report = data.reportService.tripReport(D14, D16, data.blue, TripStatus.CANCELLED);
        assertEquals(List.of(cancelledBlue), report.getTrips());
    }

    @Test
    void tripCountsByStatus() {
        TripReport report = data.reportService.tripReport(D14, D16, null, null);
        assertEquals(1, report.count(TripStatus.COMPLETED));
        assertEquals(1, report.count(TripStatus.SCHEDULED));
        assertEquals(1, report.count(TripStatus.DELAYED));
        assertEquals(1, report.count(TripStatus.CANCELLED));
    }

    @Test
    void tripRowsAreInDepartureOrderWithPlainValues() {
        TripReport report = data.reportService.tripReport(D15, D15, null, null);
        assertEquals(List.of("Trip", "Line", "From", "To", "Train", "Departure", "Arrival", "Status", "Delay (min)"),
                report.getColumns());
        assertEquals(Arrays.asList("TRP-0003", "Blue", "F", "H", "T-102",
                "2026-11-15 23:30", "2026-11-16 00:10", "Delayed", "10"),
                Arrays.asList(report.getRows().get(1)));
    }

    @Test
    void nothingMatchingGivesNoRows() {
        TripReport report = data.reportService.tripReport(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), null, null);
        assertTrue(report.getRows().isEmpty());
        assertEquals(0, report.count(TripStatus.SCHEDULED));
    }

    @Test
    void startAfterEndIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> data.reportService.tripReport(D16, D14, null, null));
        assertEquals("The start date must not be after the end date", e.getMessage());
        ValidationException missing = assertThrows(ValidationException.class,
                () -> data.reportService.revenueReport(null, D14));
        assertEquals("Choose a start date and an end date", missing.getMessage());
    }

    // ----- ticket report (4b) -----

    @Test
    void ticketsAreCountedPerDirectedJourney() {
        List<JourneyCount> journeys = data.reportService.ticketReport(D15, D15).getJourneys();
        assertEquals(3, journeys.size());
        assertSame(data.stationA, journeys.get(0).getFrom());
        assertSame(data.stationG, journeys.get(0).getTo());
        assertEquals(2, journeys.get(0).getTickets());
        assertEquals(16.00, journeys.get(0).getRevenue());
        // a tie on the number of tickets is ordered by station names: A to E before G to A
        assertSame(data.stationE, journeys.get(1).getTo());
        assertEquals(4.00, journeys.get(1).getRevenue());
        assertSame(data.stationG, journeys.get(2).getFrom());
        assertEquals(8.00, journeys.get(2).getRevenue());
    }

    @Test
    void refundedTicketsAreLeftOutOfTheTicketCount() {
        assertEquals(4, data.reportService.ticketReport(D15, D15).getTotalTickets());
        assertEquals(5, data.reportService.ticketReport(D13, D15).getTotalTickets());
    }

    @Test
    void ticketRowsArePlainText() {
        TicketReport report = data.reportService.ticketReport(D15, D15);
        assertEquals(List.of("From", "To", "Tickets", "Revenue"), report.getColumns());
        assertEquals(Arrays.asList("A", "G", "2", "16.00"), Arrays.asList(report.getRows().get(0)));
    }

    // ----- revenue report (4c) -----

    @Test
    void everyDayInRangeIsListed() {
        List<DayRevenue> days = data.reportService.revenueReport(D13, D15).getDays();
        assertEquals(3, days.size());
        assertEquals(D14, days.get(1).getDate());
        assertEquals(0, days.get(1).getTicketsSold());
        assertEquals(0.00, days.get(1).getNet());
    }

    @Test
    void salesRefundsAndNet() {
        DayRevenue day = data.reportService.revenueReport(D15, D15).getDays().get(0);
        assertEquals(5, day.getTicketsSold());
        assertEquals(36.00, day.getSales());
        assertEquals(1, day.getRefundCount());
        assertEquals(8.00, day.getRefunds());
        assertEquals(28.00, day.getNet());
    }

    @Test
    void revenueTotals() {
        RevenueReport report = data.reportService.revenueReport(D13, D15);
        assertEquals(6, report.getTotalTicketsSold());
        assertEquals(44.00, report.getTotalSales());
        assertEquals(8.00, report.getTotalRefunds());
        assertEquals(36.00, report.getNet());
    }

    @Test
    void endDayIsFullyIncluded() {
        // the 23:59 ticket is part of the 15th, and not of the 16th
        assertEquals(5, data.reportService.revenueReport(D14, D15).getTotalTicketsSold());
        assertEquals(0, data.reportService.revenueReport(D16, D16).getTotalTicketsSold());
    }

    @Test
    void revenueRowsEndWithATotal() {
        List<String[]> rows = data.reportService.revenueReport(D13, D15).getRows();
        assertEquals(4, rows.size());
        assertEquals(Arrays.asList("2026-11-15", "5", "36.00", "8.00", "28.00"), Arrays.asList(rows.get(2)));
        assertEquals(Arrays.asList("Total", "6", "44.00", "8.00", "36.00"), Arrays.asList(rows.get(3)));
    }

    // ----- export -----

    @Test
    void exportOfAnEmptyReportHasOnlyTheHeader() throws IOException {
        Path file = dir.resolve("empty.csv");
        data.reportService.tripReport(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), null, null).exportCsv(file);
        assertEquals(List.of("Trip,Line,From,To,Train,Departure,Arrival,Status,Delay (min)"),
                Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    void awkwardValuesAreQuotedInTheExport() throws IOException {
        Line odd = new Line(data.lines.nextId(), "Line \"A\", north", "#000000",
                List.of(data.stationX, data.stationY));
        data.lines.save(odd);
        trip(data.train2, odd, on(15, 6, 0), TripStatus.SCHEDULED, 0);
        Path file = dir.resolve("trips.csv");
        data.reportService.tripReport(D15, D15, odd, null).exportCsv(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains(",\"Line \"\"A\"\", north\",X,Y,"), lines.get(1));
    }

    @Test
    void revenueExportUsesPlainMoneyAndDates() throws IOException {
        Path file = dir.resolve("revenue.csv");
        data.reportService.revenueReport(D15, D15).exportCsv(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals("Date,Tickets sold,Sales,Refunds,Net", lines.get(0));
        assertEquals("2026-11-15,5,36.00,8.00,28.00", lines.get(1));
        assertEquals("Total,5,36.00,8.00,28.00", lines.get(2));
    }

    // ----- authorization -----

    @Test
    void onlyReportViewersMayGenerate() {
        for (metro.model.User user : List.of(data.agent, data.controller, data.supervisor)) {
            data.loginAs(user);
            assertThrows(AccessDeniedException.class, () -> data.reportService.tripReport(D14, D15, null, null));
            assertThrows(AccessDeniedException.class, () -> data.reportService.ticketReport(D14, D15));
            assertThrows(AccessDeniedException.class, () -> data.reportService.revenueReport(D14, D15));
        }
    }

    @Test
    void generatingAReportIsAudited() {
        data.reportService.revenueReport(D13, D15);
        List<String> log = data.lines("audit.log");
        assertTrue(log.get(log.size() - 1).contains("|admin|REPORT_GENERATED|Revenue report 2026-11-13 to 2026-11-15"));
    }
}
