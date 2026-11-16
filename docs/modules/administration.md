# Administration — explain-guide

Owner: member 3. Stations, lines with their station order, and trains.

## 1. What this module does

Specification module 1:

- 1a — *"Station Management: Admins can add, update, delete, list, and search for metro stations."*
- 1b — *"Line Management: Admins can create and manage metro lines and their station sequences."*
- 1c — *"Train Management: Admins can record and update train IDs, capacities, and service status."*

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `Station` | model | A station: English and Arabic name, open or closed |
| `StationStatus` | model | Open or Closed |
| `Line` | model | A line: name, colour and an ordered list of stations |
| `Train` | model | A train: fixed id, editable train ID (code), capacity, status |
| `TrainStatus` | model | Active, In maintenance, Retired |
| `StationRepository` | repository | `stations.txt`; find by name |
| `LineRepository` | repository | `lines.txt`; turns station ids back into `Station` objects |
| `TrainRepository` | repository | `trains.txt`; find by train ID |
| `StationService` | service | Station rules: unique names, safe deletes, interchanges |
| `LineService` | service | Line rules: unique names, at least two stations, safe deletes |
| `TrainService` | service | Train rules: unique train IDs, capacity, safe status changes and deletes |
| `StationsScreen` | ui | Searchable stations list with status filter |
| `StationFormDialog` | ui | Add or edit a station |
| `LinesScreen` | ui | Lines list with colour, station count and route |
| `LineFormDialog` | ui | Edit a line: name, colour, ordered stations with add, remove, move up, move down |
| `TrainsScreen` | ui | Trains list with status filter |
| `TrainFormDialog` | ui | Add or edit a train |

## 3. OOP concepts in this module

**Encapsulation** — [Line.java](../../src/main/java/metro/model/Line.java).
The station list is private. `getStations()` returns a list that cannot be
modified, so nobody can slip a duplicate station in from outside; the only way
in is `setStations`, which refuses a station that appears twice.

**Inheritance** — `Station`, `Line` and `Train` extend `Entity` (they inherit
the id and equality). `StationRepository`, `LineRepository` and `TrainRepository`
extend `FileRepository`. `StationsScreen`, `LinesScreen` and `TrainsScreen`
extend `CrudScreen`, and their forms extend `FormDialog`.

**Polymorphism** — `CrudScreen` calls `loadItems`, `buildColumns`, `openForm`
and `deleteItem`; each screen answers differently. The same `CrudScreen.refresh`
code fills a table of stations or a table of trains.

**Abstraction** — the screens never know how stations are stored: they call
`StationService`, which uses the `Repository` methods.

**Relationships** — a `Line` *aggregates* `Station`s: it keeps references to
stations that exist on their own and can belong to several lines. Sadat is one
`Station` object on both Line 1 and Line 2 — that is what makes it an interchange.
A line does not delete its stations when it is deleted.

## 4. The data file

`data/lines.txt` (shortened):

```
#next=5
# id|name|colour|stationIds
LIN-01|Line 1|#D32F2F|STN-001,STN-002,STN-003,…,STN-035
```

| Field | Meaning |
|---|---|
| `LIN-01` | the line's id |
| `Line 1` | its name |
| `#D32F2F` | its colour (red), used on the map and in tables |
| `STN-001,STN-002,…` | its stations, in travel order, by id |

`data/trains.txt`: `TRN-001|T-101|1800|ACTIVE` — id, train ID shown to staff, capacity, status.

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| Station names are unique, ignoring case and spaces | `StationService.create`, `update` | `StationServiceTest.duplicateNameIgnoresCaseAndSpaces` |
| A station on a line cannot be deleted | `StationService.delete` | `StationServiceTest.stationOnALineCannotBeDeleted` |
| A station with ticket history cannot be deleted | `StationService.delete` | `StationServiceTest.stationWithTicketsCannotBeDeleted` |
| An interchange is a station on two or more lines | `StationService.isInterchange` | `StationServiceTest.interchangeIsDetected` |
| Renaming a station shows on every line | `Line` keeps references | `StationServiceTest.renameIsSeenThroughTheLine` |
| A line needs at least two stations | `LineService.check` | `LineServiceTest.needsTwoStations` |
| A station appears once on a line | `Line.setStations` | `LineServiceTest.noStationTwice` |
| Colours look like `#1A2B3C` | `Line.setColorHex` | `LineServiceTest.badColour` |
| The station order is saved | `LineService.update` | `LineServiceTest.reorderIsSaved` |
| A refused change changes nothing | `LineService.update`, `TrainService.update` | `LineServiceTest.refusedUpdateChangesNothing`, `TrainServiceTest.refusedUpdateChangesNothing` |
| Train IDs are unique and stored in capitals | `TrainService.create` | `TrainServiceTest.duplicateCode`, `codeIsStoredUpperCase` |
| A train ID can be changed; the id cannot | `TrainService.update` | `TrainServiceTest.codeCanBeChanged` |
| Capacity is 1 to 3000 | `Train.setCapacity` | `TrainServiceTest.capacityRange` |
| A train with upcoming trips cannot leave service | `TrainService.update` | `TrainServiceTest.trainWithUpcomingTripsCannotGoToMaintenance` |
| A train with trip history cannot be deleted | `TrainService.delete` | `TrainServiceTest.trainWithHistoryCannotBeDeleted` |
| Only admins may change the network | `session.require(MANAGE_…)` | `StationServiceTest.agentCannotCreate`, `LineServiceTest.controllerCannotEditLines` |

## 6. One action from click to file: moving a station on Line 2

1. `LinesScreen`: select Line 2, click **Edit** → `openForm` creates `LineFormDialog`.
2. The dialog lists Line 2's stations. **Move up** removes the selected station from the list and inserts it one place higher.
3. **Save** calls `LineFormDialog.save()` → `lineService.update(id, name, colour, stations)`.
4. `LineService.update` checks the permission, then the rules (name free, at least two stations), then builds a trial `Line` to check the colour and duplicates before touching the real one.
5. It calls the setters on the real `Line` and `lines.save(line)`.
6. `LineRepository.format` writes the station ids in the new order; `FileRepository` rewrites `lines.txt`.
7. The dialog closes, the screen refreshes and shows *Line updated.*

## 7. Questions you may be asked

1. **How does the system know a station is an interchange?** It counts the lines that contain it. Two or more means interchange. Nothing is stored for it, so it can never be out of date.
2. **Why can't you delete a station that is on a line?** The line would point at a station that no longer exists. We ask the admin to take it off the line first, or to mark it Closed.
3. **Why a separate "train ID" and id?** The specification lets admins update train IDs, but our record ids never change, because trips refer to them. So a train has a fixed id (`TRN-001`) and an editable code (`T-101`) shown as "Train ID".
4. **How are the stations of a line saved?** As a comma-separated list of station ids in travel order. When loading, `LineRepository` looks each id up in the `StationRepository`, which is why stations are loaded first.
5. **What happens if `lines.txt` names a station that does not exist?** Loading stops with *lines.txt line N: unknown station STN-…*.
6. **What is aggregation here?** A line holds stations that exist on their own and can be shared. If the line is deleted, the stations stay.
7. **How does a branch line work?** Line 3's Cairo University branch is entered as a second line that shares the trunk stations from Adly Mansour to Kit Kat.
8. **Why build a trial `Line` in `update`?** To check every value before the real line is changed. If any check fails, the real line is untouched and nothing is saved.
9. **Why does `getStations()` return an unmodifiable list?** Encapsulation: otherwise any class could add a duplicate station and skip the rule in `setStations`.
10. **Who can change stations?** Only roles with `MANAGE_STATIONS` (admins). Others can still list them through `VIEW_NETWORK`, for the map and ticket sales.
11. **Where did the demo stations come from?** The published station list of the Cairo Metro, cross-checked station by station against a second source.
12. *Change it:* **allow a line with one station.** Change the check in `LineService.check` from `< 2`; `needsTwoStations` would then fail and must be updated.

## 8. Try it yourself

1. Add a field "opened year" to `Station`: the class, `StationRepository.parse/format`, the header, the form and the table.
2. Add a "Delete" refusal for a line that is the only line of any station.
3. Find Sadat in `stations.txt`, then find its id in two lines of `lines.txt`.
