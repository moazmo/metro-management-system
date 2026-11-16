# Demo script

The script for discussion day, and the acceptance test of the project:
every line of the Project 8 specification, in its own order, with the
account to use, what to do and what must happen.

**Before you start**

1. Close the app. Delete the `data/` folder next to the project (or use
   *Reset demo data…* in the admin's user menu). The next start creates
   the demo data again: the Cairo Metro network, 12 trains, a week of trips
   and a week of ticket sales.
2. Start the app: `./mvnw javafx:run` (PowerShell: `.\mvnw.cmd javafx:run`).
3. Every demo account has the password `metro123`.
4. Keep the `data/` folder open in a file window: several rows below show
   a file changing.

| Account | Role |
|---|---|
| `admin` | Admin |
| `controller` | Operations controller |
| `agent` | Ticket agent |
| `supervisor` | Station supervisor |

---

## 1. Administration

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 1a | admin | **Stations** → type `sad` in the search box | Only *Sadat* remains, marked as an interchange on Line 1 and Line 2 |
| 1a | admin | **Add station** → name `Test Square`, Arabic `ميدان التجربة` → Save | New row with the next `STN-` id; `data/stations.txt` has a new last line |
| 1a | admin | **Add station** → name `test square` again | Refused inside the form: *A station named test square already exists* |
| 1a | admin | Select *Test Square* → **Edit** → status Closed → Save | Badge turns *Closed*; the file line ends with `CLOSED` |
| 1a | admin | Select *Sadat* → **Delete** → OK | Refused: *Sadat is on Line 1, Line 2. Remove it from the line first, or mark it Closed.* |
| 1a | admin | Select *Test Square* → **Delete** → OK | Row and file line are gone |
| 1b | admin | **Lines** | Four lines with colour dots, station counts and *first → last* routes |
| 1b | admin | Select *Line 2* → **Edit** → add a station, **Move up** twice → Save → open it again | The order is kept; in `data/lines.txt` the new station id sits at that position |
| 1b | admin | **Add line** with one station only | Refused: *A line needs at least two stations* |
| 1c | admin | **Trains** → **Add train** → Train ID `t-200`, capacity 1500 | Stored as `T-200`, status Active |
| 1c | admin | Edit `T-200` → Train ID `T-201` → Save | Same internal id, new Train ID shown |
| 1c | admin | Edit `T-101` → status In maintenance | Refused: *Train T-101 still has upcoming trips (n). Cancel or reassign them first.* |
| 1c | admin | Delete `T-201` | Gone (it never ran a trip). Deleting `T-101` is refused because it has trip history |

## 2. Schedule

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 2a | controller | **Trips** → **New trip** → Line 1, direction *Helwan → New El-Marg*, train `T-101`, tomorrow, departure `07:15` | Arrival is filled in by itself (34 stops × 3 minutes) |
| 2d | controller | Save it | Refused inside the form: *Train T-101 is already on trip TRP-… (06:00–07:42, Line 1). Pick another train or time.* |
| 2a | controller | Change the departure to `23:10` → Save | Trip created; `data/trips.txt` has a new line ending `SCHEDULED|0` |
| 2b | controller | Select the new trip → **Edit** → departure `23:20` → Save | Saved; times updated in the table and the file |
| 2c | controller | **Delay** → 20 minutes | Badge *Delayed*, arrival shows `+20 min`; the file line ends `DELAYED|20` |
| 2c | controller | **Complete** | Badge *Completed*; Edit, Delay, Complete and Cancel are now greyed out |
| 2b | controller | Select another future trip → **Cancel trip** → OK | Badge *Cancelled* |
| 2c | controller | Status filter → *Cancelled*; date filter → *All dates* | Only cancelled trips (one per past day, plus yours) |
| 2 | agent | **Trips** | The same list, but no *New trip* button and no row buttons |

## 3. Tickets

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 3a, 3b | agent | **Sell ticket** → From `Helwan`, To `Cairo University` | Route: *Line 1: Helwan → Sadat (18 stops)*, *Change at Sadat*, *Line 2: Sadat → Cairo University (4 stops)*; 22 stops; fare per ticket EGP 15.00 |
| 3b | agent | Switch the ticket type to Student, then Senior | Fare becomes EGP 7.50, then EGP 10.50 |
| 3a | agent | Type Standard, quantity 2 → **Sell** | Two ticket cards with their own codes; two new lines in `data/tickets.txt` ending `VALID|||`; *My sales today* lists them |
| 3c | agent | **Validate** → paste the first code in small letters → **Check** | Green *Valid*; nothing changes in the file |
| 3c | agent | **Validate** | Green *Valid — marked as used*; the file line now shows `USED` and the time |
| 3c | agent | Paste the same code → **Validate** | Red *Already used at HH:mm on dd Mon yyyy* |
| 3c | agent | Type `TKT-999999` → **Check** | Red *Ticket not found* |
| 3c | agent | Paste a code from *yesterday* (Ticket report or `tickets.txt`) that is still `VALID` | Red *Expired — sold on …* |
| 3d | supervisor | **Refunds** → the second code from 3a → **Find** | *Eligible for a refund of EGP 15.00* and the ticket's details |
| 3d | supervisor | **Refund EGP 15.00** → OK → **Find** again | *Already refunded*; the file line ends `REFUNDED||USR-004|…` |
| 3d | supervisor | Find the first code (already used) | *Already used — cannot be refunded*, Refund button greyed out |
| 3b | admin | **Fares** → edit the first tier's price to 9.00 → **Sell ticket** a short journey | The new price is used at once. Set it back to 8.00 |

## 4. Reports

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 4a | admin | **Trip report** (last seven days) | Four counters, a pie chart in the badge colours, and a table of trips |
| 4a | admin | Line → *Line 2*, Status → *Delayed* → **Generate** | Only delayed Line 2 trips |
| 4a | admin | Set *From* after *To* → **Generate** | Message *The start date must not be after the end date*; nothing else changes |
| 4b | admin | **Ticket report** | Busiest journeys first, with a bar chart of the top eight |
| 4b | admin | **Export CSV** → save → open the file in a text editor | `From,To,Tickets,Revenue` then one line per journey |
| 4c | admin | **Revenue report** for today | Sales, refunds and net revenue. Sell a ticket as agent, come back, **Generate**: sales grew by that fare. The refund from 3d appears under refunds |
| 4c | admin | Choose dates in 2020 → **Generate** | Every day listed with zeros, *No data for these dates.* in place of the chart |

## 5. Users

| Spec | Log in as | Do | Expect |
|---|---|---|---|
| 5a | — | Log in as `admin` with a wrong password | *Wrong username or password* under the fields (the same text for an unknown username) |
| 5a | admin | User menu (top right) → **Log out** | Back at the login screen |
| 5b | agent | **My profile** → change the phone → **Save** | Green *Profile saved.*; the agent's line in `data/users.txt` shows the new phone. ID, username and role are greyed and cannot be edited |
| 5b | agent | Change password with a wrong current password | *Current password is wrong* |
| 5c | each | Log in as each account | The sidebar matches the **Authorization** list below exactly |
| 5c | admin | **Staff users** → **Add user** (Ticket agent, `nour`, password `secret1`) | New `USR-005` line in `users.txt`; the password is stored only as a hash |
| 5c | admin | Deactivate `nour`, then log in as `nour` | *This account is deactivated* |
| 5c | admin | Deactivate `admin` | Refused: *The last active admin cannot be deactivated or changed* |

---

## Authorization

The sidebar each demo account must show, in this order:

| Account | Sidebar |
|---|---|
| admin | Dashboard, Network map · Stations, Lines, Trains · Trips · Sell ticket, Validate, Refunds, Fares · Trip report, Ticket report, Revenue report · Staff users, Audit log, My profile |
| controller | Dashboard, Network map · Trips · My profile |
| agent | Dashboard, Network map · Trips · Sell ticket, Validate · My profile |
| supervisor | Dashboard, Network map · Trips · Sell ticket, Validate, Refunds · My profile |

Authorization is enforced twice. The menu hides what a role may not use,
**and** every service method checks the permission again, so even a
button shown by mistake could not do anything. To show the second half:

- `UserServiceTest.agentCannotListUsers`, `TicketServiceTest.agentCannotRefund`,
  `ReportsTest.onlyReportViewersMayGenerate` call the services directly as the
  wrong role and expect *You do not have permission to do this.*
- The Trips screen is shared: the controller gets buttons, the agent gets a read-only list.

## File handling

Three moments to open a data file during the demo:

1. **Added** — after selling two tickets (3a), open `data/tickets.txt`: two new
   last lines, one per ticket, each with its own `TKT-` code.
2. **Changed** — after validating a ticket (3c), the same line now shows `USED` and a time.
3. **Removed** — after deleting *Test Square* (1a), its line is gone from
   `data/stations.txt`, and the `#next=` counter at the top did not go back,
   so its id is never reused.

Every file is plain text: one record per line, fields separated by `|`.
Saving writes a temporary file and then swaps it in, so a crash cannot
leave half a file. Damage a line by hand and start the app: a dialog names
the file and the line number instead of crashing.

## OOP tour

| Concept | Open | Say |
|---|---|---|
| Encapsulation | `model/User.java` | All fields are private. There is no `setId` and no `setUsername`, which is exactly spec 5b: everything can be changed except the ID. `setFullName` refuses a blank name |
| Inheritance | `model/StationSupervisor.java` | A supervisor *is a* ticket agent: it extends `TicketAgent`, starts from `super.getPermissions()` and adds one permission |
| Polymorphism | `model/Ticket.java`, `StudentTicket.java` | `getFare()` is written once; each subclass answers `discountRate()` differently. Sales and reports call `ticket.getFare()` without knowing the type |
| Abstraction | `repository/FileRepository.java` | One abstract class does all the file work; each record type only fills in `parse` and `format`. The same for `Report` and `ReportScreen` |
| Interfaces | `repository/Repository.java`, `service/FarePolicy.java` | The rest of the program talks to these, not to text files or to one pricing scheme |
| Relationships | `model/Line.java`, `Trip.java`, `Journey.java` | A line aggregates stations shared with other lines; a trip is associated with a train and a line; a journey is composed of its legs |
| Exceptions | `exception/` | Own hierarchy under `MetroException`. Every message is a sentence the screen shows as it is |

## Five-minute path

1. Log in as `agent`, show the short sidebar (5a, 5c).
2. Sell Helwan → Cairo University, show the route, the change at Sadat and the fare; sell 2 (3a, 3b). Open `tickets.txt`.
3. Validate the first ticket twice (3c).
4. Log in as `controller`: create a trip that clashes and see it refused (2a, 2d); delay a trip (2c).
5. Log in as `admin`: Stations search and add (1a), Lines (1b), Revenue report (4c), Staff users (5c).

## Fifteen-minute path

All sections above in order, the Authorization table with each account,
the three file-handling moments, then the OOP tour. Finish on the
Network map with Helwan → Cairo University highlighted: Line 1, the change at Sadat, Line 2.
