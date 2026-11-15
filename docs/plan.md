# Metro Management System — Build Plan

**Goal:** Build the JavaFX desktop Metro Management System described in
the design, covering every Project 8 requirement, in phases that each
end with an application that runs.

**Architecture:** Four layers (`ui` → `service` → `repository` → text
files) around a shared `model` package. Rules live in services and are
unit-tested. Screens are plain Java JavaFX classes, checked with a
screenshot tool and a written demo script.

**Tech stack:** Java 25, JavaFX 25.0.5, Maven 3.9.16 through the Maven
Wrapper 3.3.4, AtlantaFX 3.0.0 (theme), Ikonli 12.4.0 (icons), JUnit 6.1.3.
This exact combination was built, tested and launched on the team lead's
machine on 2026-11-15 before the plan was written.

**Spec:** [design.md](design.md). The plan argues from the design; read both.

## How to use this plan

27 tasks, done in order. Each task lists the files it touches, the
exact names and signatures it must produce, the rules and wording it
must implement, and the tests that prove it. Steps are checkboxes.

| Phase | Tasks | File | Owner (design §10) |
|---|---|---|---|
| 0 Skeleton, 1 Foundation | 1–7 | [plan/phase-0-1-foundation.md](plan/phase-0-1-foundation.md) | 1, 2 |
| 2 Administration | 8–11 | [plan/phase-2-administration.md](plan/phase-2-administration.md) | 3 |
| 3 Schedule | 12–14 | [plan/phase-3-schedule.md](plan/phase-3-schedule.md) | 4 |
| 4 Tickets | 15–19 | [plan/phase-4-tickets.md](plan/phase-4-tickets.md) | 5 (route planner: 1) |
| 5 Reports | 20–22 | [plan/phase-5-reports.md](plan/phase-5-reports.md) | 6 |
| 6 Polish, 7 Discussion prep | 23–27 | [plan/phase-6-7-polish-and-prep.md](plan/phase-6-7-polish-and-prep.md) | 1, everyone |

After Task 22 everything the rubric grades is finished. Tasks 23–27 are
what makes it impressive and what prepares the team for the discussion.

## Global constraints

Every task's requirements include this section.

**Toolchain**
- JDK 25. `maven.compiler.release` is 25. No other JDK is supported.
- Build only through the wrapper: `./mvnw` (Git Bash, macOS, Linux) or `.\mvnw.cmd` (PowerShell). Nobody installs Maven.
- Dependencies are exactly: `org.openjfx:javafx-controls:25.0.5`, `io.github.mkpaz:atlantafx-base:3.0.0`, `org.kordamp.ikonli:ikonli-javafx:12.4.0`, `org.kordamp.ikonli:ikonli-material2-pack:12.4.0`, and for tests `org.junit.jupiter:junit-jupiter:6.1.3`. Adding a dependency needs the team lead's agreement.
- No `module-info.java`. Root package is `metro`.

**Layers**
- `metro.ui` may use `metro.service` and `metro.model`. `metro.service` may use `metro.repository` and `metro.model`. `metro.repository` may use `metro.model`.
- `metro.model`, `metro.repository`, `metro.service`, `metro.util`, `metro.exception` never import `javafx.*`.
- AtlantaFX is imported only in `metro.ui.Theme`. Ikonli is imported only in `metro.ui.Icons`.
- A screen never reads or writes a file and never decides a business rule. It calls a service and shows the result or the error message.
- Screens are Java classes. No FXML.

**Explainability rule** (design §6) — applies to `src/main`, not to tests
- Used freely: classes, inheritance, abstract classes, interfaces, enums, exceptions, `ArrayList`, `HashMap`, `HashSet`, `EnumSet`, loops, classic `switch`, text I/O, `java.time`.
- Lambdas only as JavaFX event handlers and for the callback parameters this plan names.
- Not used: records, `var`, `.stream()`, `Optional`, reflection, annotations other than `@Override`, and own generic types other than `Repository<T>`, `FileRepository<T>` and `CrudScreen<T>`.
- Every class has a one-paragraph comment saying what it is for. Every public method that enforces a rule names the rule in a comment.

**Data**
- All data files are UTF-8 text in the data folder (`data/` next to where the app is started), one record per line, fields joined by `|` through `metro.util.Fields`.
- One object per record. To change a record: get the loaded object, call its setters, call `save`. Never build a second object with the same id (the one exception is `UserService.changeRole`, Task 5).
- Ids are created only by `Repository.nextId()` and never change. No class has a `setId`.
- Every write goes to a temporary file that then replaces the real one.
- Money is `double`, rounded to two decimals with `Money.round`, shown with `Money.format` as `EGP 8.00`.
- Times in files: `yyyy-MM-dd'T'HH:mm:ss`. On screen: dates `dd MMM yyyy`, times `HH:mm`.
- "Now" always comes from the `java.time.Clock` held by `AppContext`, never from `LocalDateTime.now()` with no argument.

**Authorization**
- Every public service method that returns or changes data starts with `session.require(Permission.X)`. The permission is the one written beside the method in its task. Where a task says "any logged-in user", the method starts with `session.requireLogin()` instead.
- The interface hides what the user may not use; the service refuses it anyway. Both are required.

**Errors and wording**
- Rule violations throw a subclass of `MetroException` whose message is a full plain-English sentence that can be shown to the user as is. Wording given in a task is exact.
- No screen shows a stack trace or an exception class name.

**Repository hygiene**
- No teammate names or student IDs in any committed file. The roster and college handouts stay untracked. Anything naming people goes in `team-private/`.
- `data/` is not committed.
- Commit messages: one imperative sentence, no trailers.
- `main` always builds and passes its tests. Each phase is built on its own branch (`phase-2-administration`, …) and merged through a pull request once its last task is done.

## Refinements to the design

Decisions taken while planning, where the design left a choice or a
detail turned out to need one:

- **Train ID.** The specification lets admins update train IDs, but no record's `id` ever changes here. A train therefore has a fixed internal `id` and an editable, unique `code` shown on screen as "Train ID" (Task 9).
- **Who sold a ticket** is stored on the ticket as the user's id, not as a `User` object, because changing a user's role replaces that user's object (Tasks 5, 17).
- **Retiring a train** or sending it to maintenance is refused while it still has upcoming trips; deleting it is refused once it has any trip history (Task 13).
- **A delay is always saved**, even when it makes the train clash with its next trip; the clash is shown as a warning to resolve (Task 13).
- **Sidebar labels** under REPORTS are "Trip report", "Ticket report", "Revenue report", so no two sidebar items share a label.
- **Network map layout** is one row per line with interchanges joined by connectors, computed from the data (Task 23).
- **Extra exception** `NotFoundException`, and an extra data file `settings.txt` for the remembered theme.

## Review focus

The design says what the system must do. These are the conditions it
does not spell out that are most likely to bite someone using it. Each
one is pinned by a test in the task named.

1. **Awkward text in any field** — a `|`, a line break, surrounding spaces, different capital letters, Arabic. Expected: saved and read back unchanged; "helwan " is the same station name as "Helwan". → Tasks 2, 3, 8
2. **A data file edited by hand, damaged, missing, or pointing at a record that no longer exists.** Expected: a clear message naming the file and line at start-up; never a stack trace, never silently dropped data. → Tasks 3, 6, 8, 12
3. **Midnight and date edges** — a ticket sold at 23:59 and checked at 00:01, a trip that departs before midnight and arrives after, a report whose end date is today. Expected: the ticket is expired, the trip is valid and conflict-checked correctly, the report includes the whole end day. → Tasks 12, 17, 18, 20
4. **Changing something that is in use** — removing a station from a line after tickets were sold to it, deactivating the last admin or yourself, changing your own role, retiring a train that still has trips. Expected: history stays readable, and the action is refused with a reason where it would break something. → Tasks 5, 13, 18
5. **Nothing there yet** — no route between two stations, a report with no rows, a dashboard on a day with no trips or sales. Expected: a plain sentence or zeros, a header-only export, no error. → Tasks 15, 20, 24

## File map

```
pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
.github/workflows/build.yml
src/main/java/metro/
  Main.java                    starts the application
  AppContext.java              builds repositories and services once
  model/                       Entity, Station, Line, Train, Trip, Ticket…, User…, enums
  exception/                   MetroException and its subclasses
  util/                        Fields, Check, Money, Dates
  repository/                  Repository, FileRepository, one repository per record type, AuditLog, Settings
  service/                     Session, AuthService, one service per module, RoutePlanner, DemoData
  service/report/              Report and the three reports
  ui/                          MetroApp, Theme, Icons, MainWindow, Sidebar, Screen, CrudScreen, FormDialog, one class per screen
  dev/Screenshots.java         renders every screen to PNG for checking and for the README
src/main/resources/metro/
  ui/app.css
  seed/network.txt             demo lines and stations
src/test/java/metro/           mirrors main; TestData.java holds shared fixtures
docs/                          design.md, plan.md, plan/, demo-script.md, modules/, screenshots/
```

## Task cycles

**Logic task** (anything under `model`, `repository`, `service`, `util`):

1. Write the tests listed in the task.
2. Run them and see them fail: `./mvnw -q test -Dtest=<TestClass>`.
3. Write the code to the task's signatures and rules.
4. Run the same command and see them pass.
5. Run everything: `./mvnw -q test`.
6. Commit with the message given.

**Screen task** (anything under `ui`):

1. Build the screen to the task's description.
2. `./mvnw -q javafx:run -Dmetro.mainClass=metro.dev.Screenshots`, then look at the PNGs named in the task under `target/screenshots/`.
3. `./mvnw -q javafx:run` and perform the task's click-through; check the data file changed as stated.
4. `./mvnw -q test`.
5. Commit with the message given.

A test listed as `name: assertion` means a JUnit test method with that
name whose body sets up what the sentence before it says and makes that
assertion. `TestData` (Task 5) provides the shared set-up.
