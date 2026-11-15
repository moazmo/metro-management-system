# Phases 6–7 — Polish and Discussion Prep (Tasks 23–27)

Part of the [build plan](../plan.md). Nothing here is required by the
rubric; all of it is what makes the project stand out and what prepares
six people to explain it.

Branches: `phase-6-polish` (Tasks 23–25), `phase-7-discussion-prep` (Tasks 26–27).

---

### Task 23: Network map

**Files**
- Create: `ui/MapLayout.java`, `ui/NetworkMapView.java`, `ui/NetworkMapScreen.java`
- Modify: `ui/SellTicketScreen.java` (a `Pick on map` button), `ui/MainWindow.java` (sidebar row), `app.css`
- Test: `ui/MapLayoutTest.java`

**Consumes:** `Line`, `LineService.listAll()` (Task 8); `Journey`, `Leg` (Task 15); `TicketService.previewJourney`, `quoteFare` (Task 18).

**Produces**
```java
public class MapLayout {                    // pure arithmetic: imports nothing from javafx, so it is unit-tested
    public static final double SPACING = 30, ROW_GAP = 120, MARGIN_LEFT = 150, MARGIN_TOP = 110;
    public MapLayout(List<Line> lines)      // lines are placed in the order given
    public double xOf(Line line, Station station)   // MARGIN_LEFT + index on that line * SPACING
    public double yOf(Line line)                    // MARGIN_TOP + row number * ROW_GAP
    public List<Station> sharedStations()           // stations on two or more of the lines
    public double getWidth(); public double getHeight();
}

public class NetworkMapView extends Pane {
    public NetworkMapView(List<Line> lines)
    public void rebuild(List<Line> lines)
    public void setOnStationClicked(Consumer<Station> handler)
    public void setSelection(Station from, Station to)   // either may be null
    public void highlight(Journey journey)               // null clears the highlight
}

public class NetworkMapScreen extends Screen { public NetworkMapScreen(AppContext context) }
```

**Drawing rules**
- One horizontal row per line, drawn as a thick stroke in the line's colour with the line's name and a colour chip at the left.
- Each station is a dot on its row: a filled circle in the line colour; an interchange is a larger white circle with a dark ring; a closed station is grey with a strike-through name.
- Station names sit above the row, rotated 45° so neighbours do not collide.
- A station that is on several lines is drawn on each of those rows, and its dots are joined by a thin dashed grey connector. Clicking any of its dots means the same station.
- The pane is as large as `MapLayout` says and sits in a scroll pane.
- `highlight(journey)`: everything not on the journey fades to 25% opacity; each leg's stretch of its row stays bright and thicker; the connector at each change stays bright; origin and destination get a ring in the accent colour.
- Nothing is hand-placed: the picture is rebuilt from the lines every time the screen opens, so it follows any edit an admin makes.

**Network map screen**
- The map fills the screen; a panel on the right. First click sets From, second click sets To and plans the journey, a third click starts again. A `Clear` button resets.
- The panel shows: From, To, each leg (`<line>: <from> → <to> (<n> stops)`), `Change at <station>`, total stops and changes, and the fare for each ticket type. A missing route shows the planner's sentence in the panel.
- The journey comes from `TicketService.previewJourney` and the fares from `quoteFare`, so every role with `VIEW_NETWORK` can use the screen, including roles that cannot sell.
- In Sell ticket, `Pick on map` opens the same map in a dialog; choosing two stations fills From and To.

**Tests**
```
MapLayoutTest   (fixture lines Red, Blue, Green, Island in that order)
  stationsAreEvenlySpacedAlongTheirLine: xOf(red, B) - xOf(red, A) == SPACING; xOf(red, E) == MARGIN_LEFT + 4 * SPACING
  eachLineHasItsOwnRow:                  yOf(blue) - yOf(red) == ROW_GAP; yOf(island) == MARGIN_TOP + 3 * ROW_GAP
  sharedStationHasAPositionOnEachLine:   xOf(red, C) == MARGIN_LEFT + 2 * SPACING; xOf(blue, C) == MARGIN_LEFT + 1 * SPACING
  sharedStationsAreFound:                sharedStations() contains exactly C, H, E
  sizeFitsTheLongestLine:                getWidth() >= MARGIN_LEFT + 4 * SPACING; getHeight() >= MARGIN_TOP + 3 * ROW_GAP
  noLinesMeansAnEmptyMap:                new MapLayout(emptyList) -> sharedStations().isEmpty(), no exception
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement `MapLayout` · [ ] run, see them pass
- [ ] Build `NetworkMapView`, the screen and the `Pick on map` dialog; add the sidebar row
- [ ] Screenshots run. Check `admin-network-map.png`: four coloured rows, readable names, interchanges ringed and joined; `agent-network-map.png` exists.
- [ ] Click-through: click Helwan then a Line 3 station → the route lights up across rows through the interchanges, the panel lists the legs and three fares. As `admin`, close a station and reopen the map → it is grey. Add a station to a line → it appears on the map without any other change.
- [ ] Full suite · [ ] Commit `Add interactive network map`

---

### Task 24: Dashboard, dark mode that sticks, empty states

**Files**
- Create: `service/DashboardService.java`, `repository/Settings.java`, `ui/KpiTile.java`
- Modify: `ui/DashboardScreen.java`, `ui/MainWindow.java`, `ui/MetroApp.java`, `ui/Theme.java`, `AppContext.java`, `TestData.java`, `app.css`
- Test: `service/DashboardServiceTest.java`, `repository/SettingsTest.java`

**Produces**
```java
public class DashboardService {
    public DashboardService(TripRepository trips, TicketRepository tickets, Session session, Clock clock)
    public int tripsToday()                       // VIEW_TRIPS; departing today, not Cancelled
    public int delayedToday()                     // VIEW_TRIPS
    public List<Trip> nextDepartures(int limit)   // VIEW_TRIPS; Scheduled or Delayed, departing from now on, earliest first
    public int ticketsSoldToday()                 // VIEW_REPORTS
    public double revenueToday()                  // VIEW_REPORTS; today's sales minus today's refunds
    public int mySalesToday()                     // SELL_TICKETS; tickets sold today by the logged-in user
}

public class Settings {                           // data/settings.txt, lines of key=value
    public Settings(Path file)
    public String get(String key, String fallback)
    public void set(String key, String value)     // writes the file
}

public class KpiTile extends VBox { public KpiTile(String label, String value, String icon) }
```

**Dashboard**
- A greeting line: `Good morning|afternoon|evening, <full name>` by the clock (before 12:00, before 18:00, otherwise).
- Tiles, each shown only when the session has the permission its number needs: Trips today, Delayed today (`VIEW_TRIPS`); Tickets sold today, Revenue today (`VIEW_REPORTS`); My sales today (`SELL_TICKETS`, and only when the user does not have `VIEW_REPORTS`).
- Two panels under the tiles: a small `NetworkMapView` (clicking it opens Network map) and "Next departures" — the next eight trips with time, line swatch, route and status badge; with none left today it says `No more departures today.`
- The dashboard refreshes in `onShow()`.

**Dark mode**
- The toggle in the top bar calls `Theme.toggle` and stores `theme=dark` or `theme=light` through `Settings`. `MetroApp` reads it at start-up, so the choice survives a restart and applies to the login screen as well.

**Empty and error states**
- Every table has a specific empty sentence in place of the generic one: Stations `No stations match.`, Lines `No lines yet.`, Trains `No trains match.`, Trips `No trips match these filters.`, Staff users `No users match.`, Fares `No fare tiers.`, My sales today `No tickets sold yet today.`
- Go through every form once with empty and with wrong input; every refusal must be the service's sentence shown inline, never a dialog with a stack trace.

**Tests** — clock 2026-11-15T10:00
```
DashboardServiceTest
  countsTodayOnly:               trips on the 14th, 15th (one cancelled, one delayed, one scheduled), 16th
                                 -> tripsToday() == 2, delayedToday() == 1
  nextDeparturesAreUpcomingAndOrdered: trips today at 09:00, 11:00, 10:30 -> nextDepartures(8) is [10:30, 11:00]
  revenueIsSalesMinusRefunds:    today: 3 Standard at 8.00, one refunded -> ticketsSoldToday() == 3, revenueToday() == 16.00
  agentSeesOwnSalesButNotRevenue: as agent mySalesToday() works and revenueToday() throws AccessDeniedException
  emptyDayIsZeros:               no trips or tickets -> 0, 0, empty list, 0, 0.00; no exception
SettingsTest
  valueSurvivesReload:           set("theme", "dark"); new Settings(sameFile).get("theme", "light") equals "dark"
  missingFileGivesFallback
  unknownLinesAreIgnored:        a file containing "garbage" and "theme=dark" -> get("theme", "light") equals "dark"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement the service and `Settings` · [ ] run, see them pass
- [ ] Rebuild the dashboard; wire the theme setting; replace the empty-state sentences
- [ ] Screenshots run. Check all four `*-dashboard.png` files: admin sees four tiles including revenue; agent sees Trips today, Delayed today and My sales today and no revenue; controller sees only the two trip tiles. Run the screenshot tool once more after toggling dark mode in the app to confirm the dark login screen.
- [ ] Full suite · [ ] Commit `Add live dashboard and remembered dark mode`

---

### Task 25: Audit log screen and demo-data reset

**Files**
- Create: `ui/AuditLogScreen.java`
- Modify: `service/DemoData.java`, `ui/MainWindow.java` (sidebar row; `Reset demo data…` in the user menu), `ui/MetroApp.java`
- Test: add cases to `service/DemoDataTest.java`

**Produces**
```java
// DemoData
public static final List<String> DATA_FILES;      // users.txt, stations.txt, lines.txt, trains.txt, trips.txt,
                                                  // fares.txt, tickets.txt, audit.log, settings.txt
public static void reset(Path dataDir)            // deletes exactly those files from dataDir, nothing else
```

**Behaviour**
- **Audit log** (`Screen`, `VIEW_AUDIT_LOG`): table Time, User, Action, Detail, newest first; a search box matching any column; a count `Showing <n> of <total>`.
- **Reset demo data…** in the user menu, visible only with `MANAGE_USERS`. It asks: `This deletes all current data and restores the demo data. This cannot be undone. Continue?` On OK: `DemoData.reset`, a new `AppContext` on the same folder (which reseeds), and back to the login screen.
- `reset` touches only the files named in `DATA_FILES`; any other file in the folder is left alone.

**Tests**
```
DemoDataTest (added)
  resetRestoresDemoData:       add a station "Temp" and deactivate "agent"; reset(dir); new AppContext
                               -> no station "Temp"; agent active; 4 users
  resetLeavesOtherFilesAlone:  a file notes.txt in the data folder still exists after reset
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement `reset` · [ ] run, see them pass
- [ ] Build the screen and the menu item
- [ ] Click-through as `admin`: do three different actions, open Audit log → they are the top three rows with the right user. Search `TICKET` → only ticket actions. Reset demo data → confirm → login screen; log in → the demo network and timetable are back.
- [ ] Full suite · [ ] Commit `Add audit log screen and demo data reset`
- [ ] Re-run the whole `docs/demo-script.md` once more, since Tasks 23–25 touched shared screens.
- [ ] Push, pull request for `phase-6-polish`, green check, merge.

---

### Task 26: Module explain-guides

**Files**
- Create: `docs/modules/foundation.md`, `docs/modules/users.md`, `docs/modules/administration.md`, `docs/modules/schedule.md`, `docs/modules/tickets.md`, `docs/modules/reports.md`

One guide per owner (design §10). Each is written for a student who
will stand in front of an examiner, and has exactly these sections:

1. **What this module does** — the specification lines it covers, quoted.
2. **Classes** — a table: class, layer, one sentence on what it is for.
3. **OOP concepts in this module** — for each of encapsulation, inheritance, polymorphism, abstraction and relationships: the class and method that shows it, a link to the file, and two sentences on why it counts.
4. **The data file** — three real lines from the module's file with each field labelled.
5. **Rules and where they live** — a table: rule, the method that enforces it, the test that proves it.
6. **One action from click to file** — a numbered trace of one real action through screen, service, repository and file (for example "Sell a ticket").
7. **Questions you may be asked** — at least ten, with model answers in the student's own plain words. Include "why did you design it this way" questions, "what happens if" questions, and at least two that ask the student to change something small.
8. **Try it yourself** — three small exercises that make the owner read and modify their own module (for example "add a Child ticket type with 75% off").

Which classes go in which guide:

| Guide | Classes |
|---|---|
| foundation | `Main`, `AppContext`, `Entity`, `Identifiable`, exceptions, `Fields`, `Check`, `Money`, `Dates`, `Repository`, `FileRepository`, `DemoData`, `MetroApp`, `Theme`, `Icons`, `MainWindow`, `Sidebar`, `NavItem`, `Screen`, `CrudScreen`, `FormDialog`, `StatusBadge`, `RoutePlanner`, `Journey`, `Leg`, `MapLayout`, `NetworkMapView`, `NetworkMapScreen`, `DashboardService`, `DashboardScreen`, `KpiTile`, `Settings` |
| users | `User` and its four subclasses, `Role`, `Permission`, `UserFactory`, `PasswordHasher`, `UserRepository`, `Session`, `AuthService`, `UserService`, `AuditLog`, `AuditEntry`, `LoginScreen`, `StaffUsersScreen`, `UserFormDialog`, `ProfileScreen`, `AuditLogScreen` |
| administration | `Station`, `StationStatus`, `Line`, `Train`, `TrainStatus`, their repositories and services, their screens and dialogs |
| schedule | `Trip`, `TripStatus`, `Direction`, `TripRepository`, `TripService`, `ScheduleConflictException`, `TripsScreen`, `TripFormDialog` |
| tickets | `Ticket` and its three subclasses, `TicketType`, `TicketStatus`, `TicketFactory`, `TicketCheck`, `TicketRepository`, `FareTier`, `FarePolicy`, `TieredFarePolicy`, `FareTierRepository`, `FareService`, `TicketService`, the four ticket screens, `TicketCard` |
| reports | `Report` and its three subclasses, `JourneyCount`, `DayRevenue`, `ReportService`, `ReportScreen` and its three subclasses |

- [ ] Write the six guides from the code as it is, not from this plan
- [ ] Check: every class under `src/main/java/metro` appears in exactly one guide's class table (list the classes with `git ls-files src/main/java` and tick them off); every file link opens; every rule in section 5 names a test that exists.
- [ ] Each owner reads their guide, does the three exercises on a scratch branch, and reports any answer they could not follow. Rewrite those answers.
- [ ] Commit `Add module guides for the discussion`

---

### Task 27: README, class diagram, rehearsal, release

**Files**
- Create: `docs/class-diagram.md`, `docs/screenshots/*.png`
- Modify: `README.md`
- Local only, never committed: `team-private/cover-sheet.pdf`, `team-private/owners.md`

- [ ] **Screenshots.** Run the screenshot tool in light and dark mode; copy the best eight into `docs/screenshots/` (login, dashboard, network map with a highlighted route, trips, sell ticket with a ticket card, validate verdict, revenue report, staff users).
- [ ] **Class diagram.** `docs/class-diagram.md`: Mermaid class diagrams written from the finished code — one overview (model classes and their relationships) and one per layer. Every inheritance arrow and association in the diagram must exist in the code.
- [ ] **README.** Sections in this order: one-paragraph description with the dashboard screenshot; feature list grouped by the five modules; screenshot grid; "Run it" (JDK 25, one command); demo accounts table with the shared demo password; how the data files work, with a three-line sample; project structure; links to the design, the demo script, the class diagram and the module guides; the build badge. No names or student IDs.
- [ ] **Clean-machine check.** Clone the repository into a new empty folder and run `./mvnw javafx:run` there. It must start, seed demo data, and let `admin` log in. Fix anything that only worked because of files on the development machine.
- [ ] **Rehearsal.** One person presents the fifteen-minute path of `docs/demo-script.md` from the clean clone while another times it and plays examiner using the "Questions you may be asked" sections. Repeat until it fits the time and nothing surprises the presenter. Note the laptop's display scaling and window size that looked best.
- [ ] **Cover sheet.** Fill the college cover sheet (project number 8, project name `Metro Management System`, the six members' Arabic names and IDs from the roster) and save it as `team-private/cover-sheet.pdf`. Check the printed Arabic by eye; if the text does not render correctly, fill only the number, project name and IDs and write the names by hand. `team-private/owners.md` records which member owns which module.
- [ ] **Release.** Full suite green on `main`. Confirm `git status --short --ignored` shows the roster, the college PDFs, `data/` and `team-private/` as ignored and nothing private as tracked. Tag `v1.0.0` and push the tag.
- [ ] Commit `Add README with screenshots and class diagram`; push, pull request for `phase-7-discussion-prep`, green check, merge.

---

## After this plan

Packaging the application so it runs without Java installed, and the
showcase website (design §12), get their own plan once `v1.0.0` exists.
