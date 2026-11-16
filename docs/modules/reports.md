# Reports — explain-guide

Owner: member 6. Trip, ticket and revenue reports, their charts and CSV export.

## 1. What this module does

Specification module 4:

- 4a — *"Trip Reports: Admins can generate reports on trips by date, line, and service status."*
- 4b — *"Ticket Reports: Admins can view the number of tickets sold for each journey."*
- 4c — *"Revenue Reports: The system can summarize recorded ticket revenue for a selected period."*

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `Report` | service | Abstract report: date range, columns, rows, CSV export |
| `TripReport` | service | Trips by date, line and status; counts per status |
| `TicketReport` | service | Tickets per journey (from → to), busiest first |
| `RevenueReport` | service | Sales, refunds and net revenue per day, with totals |
| `JourneyCount` | service | One row of the ticket report |
| `DayRevenue` | service | One row of the revenue report |
| `ReportService` | service | Creates the reports for report viewers and logs it |
| `ReportScreen` | ui | Abstract report screen: dates, Generate, Export, table, empty state |
| `TripReportScreen` | ui | Line and status filters, counters, pie chart |
| `TicketReportScreen` | ui | Counters and a bar chart of the busiest journeys |
| `RevenueReportScreen` | ui | Sales, refunds, net figures and a bar chart per day |

## 3. OOP concepts in this module

**Abstraction** — [Report.java](../../src/main/java/metro/service/report/Report.java)
is abstract with two abstract methods, `getColumns()` and `getRows()`. Each
report fills them in its own way.

**Polymorphism** — `Report.exportCsv` is written once and works for every
report, because it only calls `getColumns()` and `getRows()`. `ReportScreen`
fills its table the same way for all three reports. `ReportService.record(Report)`
takes the abstract type, so one method logs any report.

**Inheritance** — `TripReport`, `TicketReport` and `RevenueReport` extend
`Report` and inherit the date-range check, `inRange` and the export.
`TripReportScreen`, `TicketReportScreen` and `RevenueReportScreen` extend
`ReportScreen`, which extends `Screen`.

**Encapsulation** — `JourneyCount.add` and `DayRevenue.addSale` are package
private: only the reports themselves can change their rows. Other code reads them
through getters.

**Relationships** — a `TicketReport` *is composed of* `JourneyCount` rows and a
`RevenueReport` of `DayRevenue` rows: they are created by the report and belong
to it. Reports *use* trips and tickets without owning them.

## 4. The data file

Reports read `trips.txt` and `tickets.txt` and write a CSV file when exported.
A revenue export (`revenue-report-2026-11-10-to-2026-11-16.csv`):

```
Date,Tickets sold,Sales,Refunds,Net
2026-11-10,54,552.00,20.00,532.00
Total,429,4311.80,113.00,4198.80
```

| Field | Meaning |
|---|---|
| `Date` | one row for every day of the period, even days with nothing |
| `Tickets sold` | tickets sold that day, including those refunded later |
| `Sales` | the fares of those tickets |
| `Refunds` | fares given back that day |
| `Net` | sales minus refunds |
| `Total` | the last row adds the period up |

A value containing a comma or a quote is wrapped in quotes, with inner quotes doubled.

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| The start date must not be after the end date | `Report` constructor | `ReportsTest.startAfterEndIsRejected` |
| Both end days count fully | `Report.inRange` | `ReportsTest.tripRangeIncludesBothEnds`, `endDayIsFullyIncluded` |
| A trip belongs to its departure day | `TripReport` constructor | `ReportsTest.lateEveningTripBelongsToItsDepartureDay` |
| Line and status filters combine | `TripReport` constructor | `ReportsTest.tripFiltersCombine` |
| Tickets are counted per directed journey | `TicketReport.countFor` | `ReportsTest.ticketsAreCountedPerDirectedJourney` |
| Refunded tickets are not counted as sold journeys | `TicketReport` constructor | `ReportsTest.refundedTicketsAreLeftOutOfTheTicketCount` |
| Sales count on the sale day, refunds on the refund day | `RevenueReport` constructor | `ReportsTest.salesRefundsAndNet`, `revenueTotals` |
| Every day of the period is listed | `RevenueReport` constructor | `ReportsTest.everyDayInRangeIsListed` |
| CSV quotes awkward values | `Report.csvLine` | `ReportsTest.awkwardValuesAreQuotedInTheExport` |
| An empty report exports just the header | `Report.exportCsv` | `ReportsTest.exportOfAnEmptyReportHasOnlyTheHeader` |
| Only admins may generate reports | `session.require(VIEW_REPORTS)` | `ReportsTest.onlyReportViewersMayGenerate` |

## 6. One action from click to file: exporting the revenue report

1. `RevenueReportScreen` opens; `ReportScreen.onShow()` runs `generateNow()` once for the last seven days.
2. `generateNow` calls the subclass's `generate(from, to)` → `reportService.revenueReport(from, to)`.
3. `ReportService` checks `VIEW_REPORTS`, creates `new RevenueReport(tickets.findAll(), from, to)` and logs `REPORT_GENERATED`.
4. The `RevenueReport` constructor (through `Report`) checks the dates, makes a `DayRevenue` for every day, then adds each ticket's fare on its sale day and, if refunded, on its refund day.
5. `ReportScreen` builds the table from `getColumns()` and `getRows()`, and asks the subclass for its figures (`buildSummary`) and chart (`buildChart`).
6. **Export CSV** opens a save dialog, then `exportTo(path)` → `report.exportCsv(path)` writes the header and each row.

## 7. Questions you may be asked

1. **Why is `Report` abstract?** All reports share a date range and an export, but each has its own columns and rows. The abstract methods force every report to provide them.
2. **How does one export method work for three reports?** It only uses `getColumns()` and `getRows()`, which every report must have. That is polymorphism.
3. **Why does the revenue report count a refunded ticket's sale?** Money was taken that day. The refund is a separate event on its own day. Net = sales − refunds, so the totals are honest per day.
4. **Why does the ticket report leave refunded tickets out?** It counts journeys sold; a refunded sale was undone.
5. **Is A → G the same journey as G → A?** No. They are counted separately, because they are different tickets.
6. **What happens when nothing matches?** The table is empty with *No data for these dates.*, the chart area says the same, and an export has only the header line.
7. **Why are dates in the export written as `2026-11-15`?** That form sorts correctly and spreadsheet programs read it.
8. **Who can open reports?** Roles with `VIEW_REPORTS` — admins. The menu hides them for others, and `ReportService` refuses them anyway.
9. **Why are `JourneyCount.add` and `DayRevenue.addSale` not public?** Encapsulation: only the report that owns the rows may change them.
10. **How are the charts made?** With JavaFX's own `PieChart` and `BarChart`, filled from the report objects. The pie uses the same colours as the status badges.
11. **How does the date range include the whole end day?** `inRange` compares dates, not times, so a ticket at 23:59 on the end day is included.
12. *Change it:* **add a "Refunds" column to the ticket report.** Count refunded tickets in `JourneyCount`, add the column in `getColumns()` and the value in `getRows()`; the table and the export follow by themselves.

## 8. Try it yourself

1. Add a fourth report, "Train report": trips per train in the period. Extend `Report`, add a screen extending `ReportScreen`, and a sidebar entry.
2. Show the average fare in the revenue report's figures.
3. Export the trip report, open it in a spreadsheet and check the delayed trips against the Trips screen.
