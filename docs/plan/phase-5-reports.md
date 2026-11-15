# Phase 5 — Reports (Tasks 20–22)

Part of the [build plan](../plan.md). Covers spec lines 4a, 4b, 4c, and
closes with a walk through every line of the specification.
Branch: `phase-5-reports`.

Result: admins generate trip, ticket and revenue reports with filters,
charts and export. When Task 22 is done, everything the rubric grades
is finished.

---

### Task 20: Report logic

**Files**
- Create: `service/report/Report.java`, `service/report/TripReport.java`, `service/report/TicketReport.java`, `service/report/JourneyCount.java`, `service/report/RevenueReport.java`, `service/report/DayRevenue.java`, `service/ReportService.java`
- Modify: `AppContext.java`, `TestData.java`
- Test: `service/report/TripReportTest.java`, `service/report/TicketReportTest.java`, `service/report/RevenueReportTest.java`, `service/report/ReportCsvTest.java`, `service/ReportServiceTest.java`

**Consumes:** `Trip`, `TripRepository` (Task 12); `Ticket`, `TicketRepository` (Task 17).

**Produces**
```java
public abstract class Report {
    protected Report(String title, LocalDate from, LocalDate to)
    public String getTitle(); public LocalDate getFrom(); public LocalDate getTo();
    public abstract List<String> getColumns();
    public abstract List<String[]> getRows();        // text, in display order; the same rows fill the table and the export
    public final void exportCsv(Path file)           // header row, then getRows(); UTF-8
    protected final boolean inRange(LocalDateTime time)   // time's date is from..to, both included
}

public class TripReport extends Report {
    public TripReport(List<Trip> allTrips, LocalDate from, LocalDate to, Line line, TripStatus status)  // line, status may be null
    public List<Trip> getTrips()                     // the matching trips, earliest departure first
    public int count(TripStatus status)              // among the matching trips
    // columns: Trip, Line, From, To, Train, Departure, Arrival, Status, Delay (min)
}

public class JourneyCount {
    public Station getFrom(); public Station getTo(); public int getTickets(); public double getRevenue();
}

public class TicketReport extends Report {
    public TicketReport(List<Ticket> allTickets, LocalDate from, LocalDate to)
    public List<JourneyCount> getJourneys()          // most tickets first; ties by from name, then to name
    public int getTotalTickets()
    // columns: From, To, Tickets, Revenue
}

public class DayRevenue {
    public LocalDate getDate(); public int getTicketsSold(); public double getSales();
    public int getRefundCount(); public double getRefunds(); public double getNet();
}

public class RevenueReport extends Report {
    public RevenueReport(List<Ticket> allTickets, LocalDate from, LocalDate to)
    public List<DayRevenue> getDays()                // one entry for every day from..to, including days with nothing
    public int getTotalTicketsSold(); public double getTotalSales(); public double getTotalRefunds(); public double getNet();
    // columns: Date, Tickets sold, Sales, Refunds, Net — and a last row "Total"
}

public class ReportService {
    public ReportService(TripRepository trips, TicketRepository tickets, Session session, AuditLog audit)
    public TripReport tripReport(LocalDate from, LocalDate to, Line line, TripStatus status)   // VIEW_REPORTS
    public TicketReport ticketReport(LocalDate from, LocalDate to)                             // VIEW_REPORTS
    public RevenueReport revenueReport(LocalDate from, LocalDate to)                           // VIEW_REPORTS
}
```

**Rules and wording**
- `from` after `to` → `The start date must not be after the end date`. Either date missing → `Choose a start date and an end date`.
- Trip report: a trip belongs to the date of its departure.
- Ticket report: counts tickets sold in the range, grouped by the directed pair from → to. Refunded tickets are left out, because a refunded sale was undone. Revenue is the sum of the fares of the counted tickets.
- Revenue report: a sale counts on the day it was sold, at its fare, whether or not it was refunded later. A refund counts on the day it was refunded. Net = sales − refunds.
- Row text: money is a plain number with two decimals (`8.00`), dates are `yyyy-MM-dd`, date-times `yyyy-MM-dd HH:mm`. The large figures on screen use `Money.format`; the rows do not.
- CSV: values are separated by commas; a value containing a comma, a double quote or a line break is wrapped in double quotes with inner quotes doubled.
- `ReportService` records `REPORT_GENERATED` in the audit log with the report title and the range.

**Tests** — fixture network and users from `TestData`
```
TripReportTest      (trips: Red 14th 09:00 COMPLETED; Red 15th 09:00 SCHEDULED; Blue 15th 23:30 DELAYED; Blue 16th 00:10 CANCELLED)
  rangeIncludesBothEnds:        from 14th to 15th -> 3 trips
  lateEveningTripBelongsToItsDepartureDay: from 15th to 15th contains the 23:30 trip and not the 00:10 one
  filtersCombine:               from 14th to 16th, line Blue, status CANCELLED -> exactly the 00:10 trip
  countsByStatus:               from 14th to 16th: count(COMPLETED)==1, count(SCHEDULED)==1, count(DELAYED)==1, count(CANCELLED)==1
  nothingMatchingGivesNoRows:   from 1st to 2nd -> getRows().isEmpty()
  startAfterEndIsRejected:      -> "The start date must not be after the end date"
TicketReportTest    (tickets on the 15th: A->G x3 Standard at 8.00, one of them refunded; A->E x1 Student; G->A x1 Standard)
  countsPerDirectedJourney:     journeys are A->G (2 tickets, 16.00), then A->E (1, 4.00), then G->A (1, 8.00)
  refundedTicketsAreLeftOut:    getTotalTickets() == 4
  directionMatters:             A->G and G->A are separate rows
RevenueReportTest   (same tickets; plus one Standard A->G sold on the 13th and never refunded)
  everyDayInRangeIsListed:      from 13th to 15th -> 3 days, the 14th with 0 tickets and 0.00
  salesRefundsAndNet:           the 15th: ticketsSold 5, sales 36.00, refundCount 1, refunds 8.00, net 28.00
  totals:                       from 13th to 15th: totalSales 44.00, totalRefunds 8.00, net 36.00
  endDayIsFullyIncluded:        a ticket sold on the 15th at 23:59 is counted when to == 15th
ReportCsvTest
  headerOnlyWhenEmpty:          an empty trip report exports exactly one line, the column names joined by commas
  awkwardValuesAreQuoted:       a line named  Line "A", north  is written as  "Line ""A"", north"
  moneyAndDatesArePlain:        revenue row for the 15th starts "2026-11-15,5,36.00,8.00,28.00"
ReportServiceTest
  onlyReportViewersMayGenerate: as agent, controller and supervisor each of the three methods throws AccessDeniedException;
                                as admin they return
```
Check of the arithmetic above: the 15th has five tickets — three Standard at 8.00, one Student at 4.00, one Standard at 8.00 — so sales are 36.00; one 8.00 refund leaves 28.00.

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add trip, ticket and revenue reports with export`

---

### Task 21: Report screens

**Files**
- Create: `ui/ReportScreen.java`, `ui/TripReportScreen.java`, `ui/TicketReportScreen.java`, `ui/RevenueReportScreen.java`
- Modify: `ui/MainWindow.java` (three sidebar rows under REPORTS: Trip report, Ticket report, Revenue report), `app.css`

**Produces**
```java
public abstract class ReportScreen extends Screen {
    protected ReportScreen(AppContext context, String title)
    protected abstract Report generate(LocalDate from, LocalDate to);   // called by the Generate button, inside run(...)
    protected abstract Node buildChart(Report report);                  // shown above the table
    protected void addFilter(String label, Node control)                // extra filters beside the date range
}
```

**Screens**
- Common layout: From and To date pickers (default: the last seven days ending today), extra filters, `Generate` and `Export CSV` buttons; a chart; a table whose columns and rows come from `Report.getColumns()` and `getRows()`. The report is generated once when the screen opens. With no rows the table shows `No data for these dates.` and the chart area shows the same sentence.
- `Export CSV` opens a save dialog with the default name `<title in lower case with dashes>-<from>-to-<to>.csv`, calls `exportCsv`, then shows `Saved <file name>`.
- **Trip report**: extra filters line and status (each with an `All` choice). Chart: pie of trips by status. Above the chart four small counters: Scheduled, Delayed, Completed, Cancelled.
- **Ticket report**: chart: horizontal bars of the ten busiest journeys (`<from> → <to>`). Counter: total tickets.
- **Revenue report**: three large figures — Sales, Refunds, Net (`Money.format`). Chart: bars of net revenue per day.

- [ ] Build the base class and the three screens; add the sidebar rows
- [ ] Screenshots run. Check `admin-trip-report.png`, `admin-ticket-report.png`, `admin-revenue-report.png` (charts drawn, figures filled) and that no `supervisor-*-report.png` exists.
- [ ] Click-through as `admin`:
  1. Trip report: last seven days → rows and a four-slice pie. Filter status Delayed → only delayed trips. Set From after To → the exact error sentence, nothing else changes.
  2. Ticket report → busiest journeys at the top. Export CSV → open the file in a text editor: header plus rows.
  3. Revenue report for today → sell one ticket as `agent`, come back as `admin`, Generate → Sales grew by that fare.
  4. A date range in the far past → `No data for these dates.` and the export has only the header.
- [ ] Full suite · [ ] Commit `Add report screens with charts and export`

---

### Task 22: Walk through the specification

**Files**
- Create: `docs/demo-script.md`
- Modify: `README.md` (status line), anything the walk-through shows to be wrong

`docs/demo-script.md` is the script for discussion day and the
acceptance test for the project. It has one row per specification line,
in the specification's order:

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 1a | admin | Stations → add, edit, search, delete a station | each change visible in the table and in `data/stations.txt` |
| … | | | |

It must contain:
- All fourteen lines 1a–1c, 2a–2d, 3a–3d, 4a–4c and all three lines 5a–5c, each with concrete steps and a concrete expected result, including the refusal cases (duplicate station, conflicting trip, used ticket, late refund).
- A section **Authorization**: for each of the four demo accounts, the exact list of sidebar items it must see (from the navigation table in Task 6), and one action each that must be refused.
- A section **File handling**: three moments where the presenter opens a data file to show a line that was just added, changed or removed.
- A section **OOP tour**: for each of encapsulation, inheritance, polymorphism, abstraction and relationships, the file and class to open and one sentence to say (design §7).
- A five-minute path (the shortest route through the rows above that still shows every module) and a fifteen-minute path.

- [ ] Write `docs/demo-script.md`
- [ ] Delete the local `data/` folder, start the app, and perform every row exactly as written with the named account. Tick each row. Anything that does not behave as written is fixed in the code (with a test when it is logic) or, if the script was wrong, in the script.
- [ ] Check each account's sidebar against the Authorization section.
- [ ] Full suite · [ ] Commit `Add demo script covering every specification line`
- [ ] Update the README status to say all Project 8 requirements are implemented. Commit `Mark all requirements implemented`.
- [ ] Push, pull request for `phase-5-reports`, green check, merge.

**Gate:** do not start Task 23 until every row of the demo script is ticked.
