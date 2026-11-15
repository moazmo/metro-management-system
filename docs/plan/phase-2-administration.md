# Phase 2 — Administration (Tasks 8–11)

Part of the [build plan](../plan.md). Covers spec lines 1a, 1b, 1c.
Branch: `phase-2-administration`.

Result: admins manage stations, lines with their station order, and
trains; the demo Cairo network is in place.

---

### Task 8: Lines, station rules, line rules

**Files**
- Create: `model/Line.java`, `repository/LineRepository.java`, `service/StationService.java`, `service/LineService.java`
- Modify: `AppContext.java`, `src/test/java/metro/TestData.java`
- Test: `repository/LineRepositoryTest.java`, `service/StationServiceTest.java`, `service/LineServiceTest.java`

**Consumes:** `Station`, `StationRepository`, `FileRepository` (Task 3); `Session`, `AuditLog`, `TestData` (Task 5).

**Produces**
```java
public class Line extends Entity {
    public Line(String id, String name, String colorHex, List<Station> stations)
    public String getName(); public void setName(String name)          // Check.notBlank(name, "Line name")
    public String getColorHex(); public void setColorHex(String hex)   // must match #RRGGBB, stored upper case
    public List<Station> getStations()                                 // unmodifiable, in travel order
    public void setStations(List<Station> stations)                    // keeps a copy
    public boolean contains(Station station); public int indexOf(Station station)
    public boolean areAdjacent(Station a, Station b)                   // next to each other, either order
    public Station getFirstStation(); public Station getLastStation(); public int size()
    public String toString()                                           // the name
}

public class LineRepository extends FileRepository<Line> {
    public LineRepository(Path file, StationRepository stations)
    // "Line", "LIN", 2 digits, header "id|name|colour|stationIds"; station ids joined with ","
    public Line findByName(String name)                 // trimmed, ignoring case
    public List<Line> findByStation(Station station)
}

public class StationService {
    public StationService(StationRepository stations, LineRepository lines, Session session, AuditLog audit)
    public List<Station> listAll()                      // VIEW_NETWORK
    public List<Station> search(String text)            // VIEW_NETWORK; name, Arabic name or id; ignoring case; blank -> all
    public Station create(String name, String nameAr)   // MANAGE_STATIONS; new stations are OPEN
    public Station update(String id, String name, String nameAr, StationStatus status)   // MANAGE_STATIONS
    public void delete(String id)                       // MANAGE_STATIONS
    public List<Line> linesOf(Station station)          // VIEW_NETWORK
    public boolean isInterchange(Station station)       // VIEW_NETWORK; on two or more lines
}

public class LineService {
    public LineService(LineRepository lines, Session session, AuditLog audit)
    public List<Line> listAll()                         // VIEW_NETWORK
    public List<Line> search(String text)               // VIEW_NETWORK; line name or any of its station names
    public Line create(String name, String colorHex, List<Station> stations)              // MANAGE_LINES
    public Line update(String id, String name, String colorHex, List<Station> stations)   // MANAGE_LINES
    public void delete(String id)                       // MANAGE_LINES
}

// TestData gains: stations, lines repositories; stationService, lineService; and the fixture network
//   Red   (#D32F2F): A B C D E
//   Blue  (#1976D2): F C G H
//   Green (#388E3C): H E I
//   Island(#757575): X Y            (not connected to the others)
// with fields stationA … stationI, stationX, stationY, red, blue, green, island.
// Stations are created in the order A … I, X, Y (STN-001 … STN-011); lines in the order Red, Blue, Green, Island.
```

**Rules and wording**
- Station name unique after trimming, ignoring case; an update may keep its own name → `A station named <name> already exists`.
- Deleting a station that is on a line → `<name> is on <line names, comma separated>. Remove it from the line first, or mark it Closed.`
- Line name unique the same way → `A line named <name> already exists`.
- Fewer than two stations → `A line needs at least two stations`.
- The same station twice → `A station can appear only once on a line`.
- Colour not `#RRGGBB` → `Colour must look like #1A2B3C`.
- `lines.txt` naming a station id that does not exist → `DataFileException` `lines.txt line <n>: unknown station <id>`.
- Audit actions: `STATION_CREATED`, `STATION_UPDATED`, `STATION_DELETED`, `LINE_CREATED`, `LINE_UPDATED`, `LINE_DELETED`; detail is the name.

**Tests**
```
LineRepositoryTest
  stationOrderSurvivesReload:      Red reloaded has stations A B C D E in that order
  sharedStationIsTheSameObject:    after reload, red.getStations().get(2) == blue.getStations().get(1)
  fileLineLooksRight:              a line of lines.txt equals "LIN-01|Red|#D32F2F|STN-001,STN-002,STN-003,STN-004,STN-005"
  unknownStationNamesFileAndLine:  station id STN-999 in the file -> DataFileException containing "lines.txt line 3: unknown station STN-999"
StationServiceTest
  agentCannotCreate:               as agent -> AccessDeniedException; as agent listAll() works
  duplicateNameIgnoresCaseAndSpaces: create("  a ", "") -> "A station named a already exists"
  updateMayKeepOwnName:            update(A.id, "A", "ألف", OPEN) succeeds
  renameIsSeenThroughTheLine:      update(A.id, "Alpha", ...) -> red.getFirstStation().getName() equals "Alpha"
  stationOnALineCannotBeDeleted:   delete(C.id) -> message contains "C is on Red, Blue"
  freeStationCanBeDeleted:         create("Z", ""); delete it; stations.txt has no "Z" line
  interchangeIsDetected:           isInterchange(C) true; isInterchange(A) false
LineServiceTest
  needsTwoStations:                create("Short", "#000000", [A]) -> "A line needs at least two stations"
  noStationTwice:                  create("Loop", "#000000", [A, B, A]) -> "A station can appear only once on a line"
  badColour:                       create("L", "red", [A, B]) -> "Colour must look like #1A2B3C"
  duplicateName:                   create("red", ...) -> "A line named red already exists"
  reorderIsSaved:                  update(red.id, "Red", "#D32F2F", [E, D, C, B, A]) -> after reload first station is E
  controllerCannotEditLines:       as controller -> AccessDeniedException
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add lines with station order and station and line rules`

---

### Task 9: Trains

**Files**
- Create: `model/Train.java`, `model/TrainStatus.java`, `repository/TrainRepository.java`, `service/TrainService.java`
- Modify: `AppContext.java`, `TestData.java`
- Test: `repository/TrainRepositoryTest.java`, `service/TrainServiceTest.java`

The specification says admins "record and update train IDs". A record's
`id` never changes in this system, so the editable train identifier is
a separate field, `code` (for example `T-101`). The examiner-facing
label for `code` on screen is **Train ID**.

**Produces**
```java
public enum TrainStatus { ACTIVE("Active"), MAINTENANCE("In maintenance"), RETIRED("Retired") }   // getDisplayName()

public class Train extends Entity {
    public Train(String id, String code, int capacity, TrainStatus status)
    public String getCode(); public void setCode(String code)        // Check.notBlank(code, "Train ID"), stored trimmed upper case
    public int getCapacity(); public void setCapacity(int capacity)  // Check.range(capacity, 1, 3000, "Capacity")
    public TrainStatus getStatus(); public void setStatus(TrainStatus status)
    public boolean isActive()
    public String toString()                                         // the code
}

public class TrainRepository extends FileRepository<Train> {
    public TrainRepository(Path file)            // "Train", "TRN", 3 digits, header "id|code|capacity|status"
    public Train findByCode(String code)         // trimmed, ignoring case
}

public class TrainService {
    public TrainService(TrainRepository trains, Session session, AuditLog audit)
    public List<Train> listAll()                 // VIEW_TRIPS
    public List<Train> listActive()              // VIEW_TRIPS
    public List<Train> search(String text)       // VIEW_TRIPS; code, id or status name
    public Train create(String code, int capacity)                              // MANAGE_TRAINS; new trains are ACTIVE
    public Train update(String id, String code, int capacity, TrainStatus status)   // MANAGE_TRAINS
    public void delete(String id)                // MANAGE_TRAINS
}
// TestData gains: trains repository, trainService, train1 (T-101, 1200), train2 (T-102, 1200), train3 (T-103, 1000, MAINTENANCE)
```

**Rules and wording**
- Code unique ignoring case; an update may keep its own → `A train with ID <code> already exists`.
- Audit actions: `TRAIN_CREATED`, `TRAIN_UPDATED`, `TRAIN_DELETED`; detail is the code.
- Rules that depend on trips (status change and delete while trips exist) are added in Task 13.

**Tests**
```
TrainRepositoryTest
  survivesReload, fileLineLooksRight: "TRN-001|T-101|1200|ACTIVE"
TrainServiceTest
  codeIsStoredUpperCase:           create(" t-200 ", 800).getCode() equals "T-200"
  duplicateCode:                   create("t-101", 800) -> "A train with ID T-101 already exists"
  codeCanBeChanged:                update(train1.id, "T-901", 1200, ACTIVE) -> same id, findByCode("T-901") is train1
  capacityRange:                   create("T-300", 0) and (…, 3001) -> "Capacity must be between 1 and 3000"
  listActiveLeavesOutMaintenance:  listActive() does not contain train3
  controllerCanListButNotCreate
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add trains with editable train ID, capacity and status`

---

### Task 10: Demo network

**Files**
- Create: `src/main/resources/metro/seed/network.txt`
- Modify: `service/DemoData.java`, `AppContext.java` (seed the network on first run)
- Test: `service/DemoDataTest.java`

**Produces**
```java
// DemoData
public static void seedNetwork(StationRepository stations, LineRepository lines, TrainRepository trains)
```

**`network.txt` format** — one line per metro line, read with `Fields.split`:
```
# name|colour|stations in travel order, each as English=Arabic, separated by ";"
Line 1|#D32F2F|Helwan=حلوان;Ain Helwan=عين حلوان; …
```
`seedNetwork` creates each station the first time its English name appears (so a station on two lines is one object), then the lines, then twelve trains `T-101` … `T-112` with capacity 1800, all Active except `T-112`, which is In maintenance.

**Content**
- Four lines: `Line 1` (#D32F2F), `Line 2` (#F9A825), `Line 3` (#388E3C) from Adly Mansour to Rod El Farag Corridor, and `Line 3 – Cairo University branch` (#2E7D32) from Adly Mansour to Cairo University. The two Line 3 entries share every station from Adly Mansour to Kit Kat (design §4: a branching line is entered as two lines).
- Station names and order are copied from the operator's current published map, with Arabic names. Before committing, compare the list with a second independent source and fix differences.

**Tests**
```
DemoDataTest
  fourLines:                         lines.findAll().size() == 4
  everyLineHasAtLeastTenStations
  stationNamesAreUnique:             no two stations share a name ignoring case
  everyStationHasAnArabicName
  interchangesAreShared:             "Sadat" and "Al Shohadaa" are on Line 1 and Line 2; "Attaba" on Line 2 and Line 3;
                                     "Nasser" on Line 1 and Line 3; "Kit Kat" on both Line 3 entries
  helwanIsATerminalOfLineOne:        Line 1's first or last station is "Helwan"
  twelveTrainsOneInMaintenance
  seedingTwiceDoesNotDuplicate:      a second AppContext on the same folder still has the same station count
```
If the published map uses a different spelling for an interchange named above, use the map's spelling in both the file and the test.

- [ ] Write the tests · [ ] run, see them fail · [ ] write `network.txt` and `seedNetwork` · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add demo metro network`

---

### Task 11: Stations, Lines and Trains screens

**Files**
- Create: `ui/StationsScreen.java`, `ui/StationFormDialog.java`, `ui/LinesScreen.java`, `ui/LineFormDialog.java`, `ui/TrainsScreen.java`, `ui/TrainFormDialog.java`
- Modify: `ui/MainWindow.java` (three sidebar rows under ADMINISTRATION), `app.css`

**Consumes:** `CrudScreen`, `FormDialog`, `StatusBadge` (Task 7); `StationService`, `LineService` (Task 8); `TrainService` (Task 9).

**Screens**
- **Stations** (`CrudScreen<Station>`, add button `Add station`): columns ID, Name, Arabic name, Lines (line names, comma separated), Interchange (`Yes` or empty), Status badge (`Open` good, `Closed` warning). Filter: status (All / Open / Closed). Form: name, Arabic name, status (status only when editing). Delete asks `Delete station <name>?`.
- **Lines** (`CrudScreen<Line>`, add button `Add line`): columns ID, Name with a colour swatch, Stations (count), Route (`<first> → <last>`). `LineFormDialog`: name; colour picker; an ordered list of the line's stations with the buttons Add (a choice of stations not yet on this line), Remove, Move up, Move down. Saving calls `LineService.create` / `update` with the list as shown.
- **Trains** (`CrudScreen<Train>`, add button `Add train`): columns ID, Train ID (the code), Capacity, Status badge (`Active` good, `In maintenance` warning, `Retired` neutral). Filter: status. Form: train ID, capacity, status (status only when editing).

- [ ] Build the three screens and add the sidebar rows
- [ ] Screenshots run. Check `admin-stations.png` (interchanges marked, Arabic names readable), `admin-lines.png` (four lines with swatches), `admin-trains.png`; and that no `agent-stations.png` or `controller-lines.png` exists.
- [ ] Click-through as `admin`:
  1. Stations → search `sad` → only Sadat. Add station `Test Square` → appears; `data/stations.txt` gains a line. Add it again → refused with the duplicate message.
  2. Lines → edit Line 2 → Add `Test Square`, Move up twice, Save → reopen: the order is kept; `data/lines.txt` shows the new id in that position.
  3. Stations → delete `Test Square` → refused, message names Line 2. Remove it from Line 2, delete again → gone from the file.
  4. Trains → add `T-200`, capacity 1500; edit it to `T-201`; set status In maintenance; delete it.
- [ ] Full suite · [ ] Commit `Add stations, lines and trains screens`
- [ ] Push, pull request for `phase-2-administration`, green check, merge.
