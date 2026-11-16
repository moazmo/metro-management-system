# Foundation — explain-guide

Owner: member 1 (team lead). Everything the other modules stand on: start-up,
file storage, the main window and menu, the shared screen classes, the route
planner, the network map and the dashboard.

## 1. What this module does

It has no specification line of its own; it makes the others possible. Parts
of it answer these lines directly:

- 3b — *"The system should calculate the fare based on the selected journey or number of stations."*
  The route planner finds the journey and its number of stations.
- 5a — *"Staff users can log in and log out of the system."* The main window and its user menu.
- 5c — *"The system should restrict features according to each user's role and permissions."*
  The sidebar shows only what the role may use.
- File handling (rubric) — *"Saving, retrieving, and modifying data in the file."* `FileRepository`.

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `Main` | start | The `main` method; launches the JavaFX application |
| `AppContext` | start | Builds every repository and service once, in the right order, and seeds demo data on the first run |
| `Identifiable` | model | Interface: anything with an id |
| `Entity` | model | Abstract base of every record; holds the final id; equality by id |
| `Journey` | model | A route: the stations passed and its legs |
| `Leg` | model | The part of a journey travelled on one line |
| `MetroException` | exception | Parent of all our errors; its message is shown to the user |
| `ValidationException` | exception | The input broke a rule |
| `NotFoundException` | exception | No record has that id |
| `AccessDeniedException` | exception | The role lacks the permission |
| `DataFileException` | exception | A data file cannot be read or written, or a line is damaged |
| `Fields` | util | Joins values into one file line with `|` and splits it back, escaping `|`, `\` and line breaks |
| `Check` | util | Small validation helpers: not blank, in range, positive |
| `Money` | util | Rounds and formats amounts (`EGP 8.00`) |
| `Dates` | util | All date and time formats in one place |
| `Repository` | repository | Interface: find, save, delete, next id |
| `FileRepository` | repository | Abstract class doing all the text-file work for any record type |
| `Settings` | repository | `settings.txt`: key=value preferences (light or dark mode) |
| `DemoData` | service | The first-run data: accounts, the Cairo network, trains, a week of trips and sales; reset |
| `RoutePlanner` | service | Breadth-first search for the route with the fewest stops |
| `DashboardService` | service | Today's figures, each behind the permission it needs |
| `MetroApp` | ui | The JavaFX application: login screen ↔ main window |
| `Theme` | ui | Light/dark look; the only class that knows the theme library |
| `Icons` | ui | Icons by name; the only class that knows the icon library |
| `MainWindow` | ui | Top bar, sidebar and current screen; the list of all sidebar entries |
| `Sidebar` | ui | Builds the menu, showing only items the user may use |
| `NavItem` | ui | One sidebar entry: label, icon, permission, screen to open |
| `Screen` | ui | Abstract base of every screen: title, message banner, `run(...)` |
| `CrudScreen` | ui | Abstract base of list screens: search, table, add/edit/delete buttons |
| `FormDialog` | ui | Abstract base of pop-up forms; a refused save keeps the form open |
| `StatusBadge` | ui | Coloured status label |
| `LineSwatch` | ui | A line's colour dot and name |
| `StationPicker` | ui | Type-to-filter station chooser |
| `KpiTile` | ui | A dashboard figure |
| `DashboardScreen` | ui | Greeting, figures, mini map, next departures |
| `MapLayout` | ui | Pure arithmetic: where each line and station goes on the map |
| `NetworkMapView` | ui | Draws the map from the lines; click and highlight |
| `NetworkMapScreen` | ui | The full map with the journey panel |
| `Screenshots` | dev | Developer tool: opens every screen for every account, runs scripted checks, saves pictures |

## 3. OOP concepts in this module

**Encapsulation** — [Entity.java](../../src/main/java/metro/model/Entity.java).
The id is `private final` and there is no `setId`. Once a record exists its id
can never change, and every subclass inherits that guarantee.

**Inheritance** — [FileRepository.java](../../src/main/java/metro/repository/FileRepository.java).
Seven repositories (`StationRepository`, `UserRepository`, `TicketRepository`…)
extend it and inherit loading, saving, finding, deleting and id numbering.
The same with `Screen` → every screen, and `CrudScreen` → Stations, Lines,
Trains, Trips, Staff users, Fares.

**Polymorphism** — `FileRepository.load()` calls `parse(fields)` for every line.
Which `parse` runs depends on the object: a `StationRepository` builds a
`Station`, a `TicketRepository` builds the right ticket subclass. One loading
method, many behaviours. `MainWindow.open(item)` calls `screen.onShow()`; each
screen refreshes in its own way.

**Abstraction** — [Repository.java](../../src/main/java/metro/repository/Repository.java)
is an interface: services talk to "a repository", not to text files. `Screen`,
`CrudScreen` and `FormDialog` are abstract: they cannot be created on their own,
only through a real screen.

**Relationships** — `AppContext` *has* every repository and service
(composition: it creates them and they live as long as it does). A `Journey` is
composed of `Leg`s that exist only for that journey. `NavItem` is associated
with the screen it opens.

## 4. The data file

Every data file follows the layout `FileRepository` writes. `data/stations.txt`:

```
#next=85
# id|name|nameAr|status
STN-001|Helwan|حلوان|OPEN
```

| Field | Meaning |
|---|---|
| `#next=85` | the next id number to give out; it never goes down, so ids are never reused |
| `# id\|name\|…` | the column names, for people reading the file; skipped when loading |
| `STN-001` | the id, made by `nextId()` |
| `Helwan` / `حلوان` | English and Arabic name |
| `OPEN` | the status (an enum name) |

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| Ids are never reused, even after deletes | `FileRepository.nextId`, `load` | `StationRepositoryTest.idsAreNeverReused` |
| A damaged line names the file and line number | `FileRepository.parseLine` | `StationRepositoryTest.damagedLineNamesFileAndLine` |
| Any text survives saving (`\|`, line breaks, Arabic) | `Fields.join`, `Fields.split` | `FieldsTest.roundTripsAwkwardText` |
| The saved file is readable text in a fixed layout | `FileRepository.flush` | `StationRepositoryTest.fileIsReadableText` |
| A file saved by Notepad with a byte-order mark still loads | `FileRepository.load` | `StationRepositoryTest.fileSavedByAnEditorWithByteOrderMarkStillLoads` |
| The first run creates the demo accounts | `AppContext` constructor | `AppContextTest.firstRunCreatesFolderAndDemoAccounts` |
| A damaged file stops start-up with a clear message | `AppContext`, `MetroApp.start` | `AppContextTest.damagedFileStopsStartUp` |
| The route has the fewest stops | `RoutePlanner.plan` | `RoutePlannerTest.picksTheShorterOfTwoWays` |
| No route is a plain sentence, not a crash | `RoutePlanner.plan` | `RoutePlannerTest.noRouteIsAPlainMessage` |
| An edited network is used straight away | `RoutePlanner.plan` | `RoutePlannerTest.editedNetworkIsUsed` |
| Dashboard figures follow permissions | `DashboardService` | `DashboardServiceTest.agentSeesOwnSalesButNotRevenue` |
| Lines are lined up at their first shared station on the map | `MapLayout` constructor | `MapLayoutTest.aLineIsShiftedSoItsFirstSharedStationLinesUp` |

## 6. One action from click to file: starting the program

1. `Main.main` calls `Application.launch(MetroApp.class)`.
2. `MetroApp.start` creates `new AppContext(Path.of("data"), clock)`.
3. `AppContext` creates the `data` folder if needed and checks whether `users.txt` exists (first run?).
4. It creates each repository and calls `load()`, in an order where everything a line refers to already exists: users, stations, lines (they refer to stations), trains, trips (they refer to trains and lines), fares, tickets.
5. `FileRepository.load` reads every line, skips `#` lines, splits each with `Fields.split` and calls the subclass's `parse`.
6. On the first run `DemoData` fills the repositories; each `save` writes `stations.txt.tmp` and moves it over `stations.txt`.
7. `AppContext` creates the services, passing the repositories and the one `Session`.
8. `MetroApp` shows `LoginScreen`. After a login it shows `MainWindow`, whose `Sidebar` asks `NavItem.isVisibleTo(session)` for each entry.

## 7. Questions you may be asked

1. **Why text files and not a database?** The rubric grades file handling, the course teaches text I/O, and anyone can open the file and watch it change.
2. **What happens if the program crashes while saving?** Nothing is lost: we write a temporary file first and then move it over the real one in one step. The old file is either fully replaced or untouched.
3. **Why is `FileRepository` abstract?** It has work it cannot do itself: turning a line into a station, a ticket or a user. Each subclass supplies that through `parse` and `format`. You cannot create a "file repository of nothing".
4. **Why is `Repository` an interface when `FileRepository` already exists?** Services depend on the interface. If we moved to a database, we would write a new class that implements `Repository` and the services would not change.
5. **How is a new id made?** `nextId()` formats the counter (`STN-` + 3 digits) and increases it. The counter is saved on the first line of the file, so it survives restarts and deletes.
6. **What if someone deletes the `#next` line by hand?** `load()` looks at every id in the file and never gives out a number below the highest one (`missingCounterLineStillGivesFreshIds`).
7. **What does `Fields.join("A|B")` write?** `A\|B`. The backslash marks the `|` as part of the value, so `split` gives back `A|B` and not two fields.
8. **How does the route planner work?** Breadth-first search: first all stations one stop away, then two stops, and so on. The first time it reaches the destination it has a route with the fewest stops. Then it walks back to the start and splits the route into legs, one per line.
9. **Why does `Screen.run(...)` exist?** Every action on every screen goes through it. A service that refuses throws a `MetroException`; `run` catches it and shows the message in the red banner, so no screen ever crashes or shows a stack trace.
10. **How does the menu know what to show?** Each `NavItem` names a permission. `Sidebar` shows an item only when `session.can(permission)` is true.
11. **Is hiding the menu enough for security?** No. Every service method checks the permission again with `session.require(...)`. Hiding is for the user; the service check is the real protection.
12. **What would you change to add a fifth role?** A new `User` subclass with its `getPermissions()`, a new `Role` value and a case in `UserFactory`. The menu and the services need no change.
13. *Change it:* **make the map spacing smaller.** Change `MapLayout.SPACING`; the map, its tests and the dashboard mini map all follow.

## 8. Try it yourself

1. In `Money.format`, show amounts as `8.00 EGP` instead of `EGP 8.00`. Run the tests: which ones fail, and why?
2. Add a station by hand to `data/stations.txt` with the next id, update `#next`, start the app and find it on the Stations screen.
3. Break a line of `data/trains.txt` (delete one field), start the app and read the message. Which method produced it?
