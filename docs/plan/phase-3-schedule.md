# Phase 3 — Schedule (Tasks 12–14)

Part of the [build plan](../plan.md). Covers spec lines 2a, 2b, 2c, 2d.
Branch: `phase-3-schedule`.

Result: authorized staff schedule trips, change and cancel them, track
their status, and the system refuses a schedule that puts one train in
two places at once.

---

### Task 12: Trips

**Files**
- Create: `model/Direction.java`, `model/TripStatus.java`, `model/Trip.java`, `repository/TripRepository.java`
- Modify: `AppContext.java`, `TestData.java`
- Test: `model/TripTest.java`, `model/TripStatusTest.java`, `repository/TripRepositoryTest.java`

**Consumes:** `Train`, `TrainRepository` (Task 9); `Line`, `LineRepository` (Task 8).

**Produces**
```java
public enum Direction { FORWARD, BACKWARD }     // FORWARD: from the line's first station to its last

public enum TripStatus {                        // getDisplayName()
    SCHEDULED("Scheduled"), DELAYED("Delayed"), COMPLETED("Completed"), CANCELLED("Cancelled");
    public boolean canChangeTo(TripStatus next)
    public boolean isFinal()                    // COMPLETED and CANCELLED
}

public class Trip extends Entity {
    public Trip(String id, Train train, Line line, Direction direction,
                LocalDateTime departure, LocalDateTime arrival)                 // SCHEDULED, no delay
    public Trip(String id, Train train, Line line, Direction direction,
                LocalDateTime departure, LocalDateTime arrival,
                TripStatus status, int delayMinutes)                            // used when loading
    public Train getTrain(); public Line getLine(); public Direction getDirection();
    public LocalDateTime getDeparture(); public LocalDateTime getArrival();
    public TripStatus getStatus(); public int getDelayMinutes();
    public void reschedule(Train train, Line line, Direction direction,
                           LocalDateTime departure, LocalDateTime arrival)
    public void changeStatus(TripStatus next)
    public void setDelayMinutes(int minutes)
    public LocalDateTime getEffectiveArrival()     // arrival plus the delay
    public Station getOrigin(); public Station getDestination()   // from the line and the direction
    public boolean overlaps(LocalDateTime otherDeparture, LocalDateTime otherEffectiveArrival, int gapMinutes)
}

public class TripRepository extends FileRepository<Trip> {
    public TripRepository(Path file, TrainRepository trains, LineRepository lines)
    // "Trip", "TRP", 4 digits, header "id|trainId|lineId|direction|departure|arrival|status|delayMinutes"
    public List<Trip> findByTrain(Train train)
    public List<Trip> findByLine(Line line)
}
// TestData gains: trips repository
```

**Rules and wording**
- Arrival not after departure (constructor and `reschedule`) → `Arrival must be after departure`.
- Allowed status changes: Scheduled → Delayed, Completed, Cancelled. Delayed → Delayed, Completed, Cancelled. Completed and Cancelled → nothing. Otherwise `changeStatus` throws `A <current display name> trip cannot become <next display name>`.
- `overlaps`: true when `departure < otherEffectiveArrival + gap` and `otherDeparture < getEffectiveArrival() + gap`. It compares times only; the caller decides which trips to compare.
- A trip file naming an unknown train or line → `trips.txt line <n>: unknown train <id>` / `unknown line <id>`.

**Tests** — times are on 2026-11-15 unless a date is given
```
TripStatusTest
  scheduledCanBecomeDelayedCompletedCancelled
  delayedCanStayDelayedOrFinish
  finalStatusesCannotChange:   COMPLETED.canChangeTo(x) and CANCELLED.canChangeTo(x) false for every x
TripTest
  arrivalMustBeAfterDeparture: 10:00 -> 10:00 and 10:00 -> 09:59 throw "Arrival must be after departure"
  overlapInsideIsDetected:     trip 10:00–10:45 overlaps(10:30, 11:00, 0) is true
  touchingTripsNeedTheGap:     trip 10:00–10:45 overlaps(10:45, 11:15, 0) false; overlaps(10:45, 11:15, 5) true
  outsideTheGapIsFine:         trip 10:00–10:45 overlaps(10:50, 11:20, 5) false
  delayExtendsTheBusyTime:     same trip with setDelayMinutes(10): overlaps(10:50, 11:20, 5) true
  tripAcrossMidnightWorks:     trip 23:50 -> 00:20 next day is accepted; overlaps(00:10 next day, 00:40 next day, 0) true
  originAndDestinationFollowDirection: Red FORWARD: A -> E; Red BACKWARD: E -> A
  finishedTripCannotBeReopened: changeStatus(COMPLETED) then changeStatus(SCHEDULED) throws
                               "A Completed trip cannot become Scheduled"
TripRepositoryTest
  survivesReloadWithStatusAndDelay
  fileLineLooksRight:          "TRP-0001|TRN-001|LIN-01|FORWARD|2026-11-15T10:00:00|2026-11-15T10:45:00|SCHEDULED|0"
  unknownTrainNamesFileAndLine: "trips.txt line 3: unknown train TRN-999"
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add trips with status rules and overlap check`

---

### Task 13: Scheduling rules and conflict detection

**Files**
- Create: `exception/ScheduleConflictException.java`, `service/TripService.java`
- Modify: `service/TrainService.java`, `service/LineService.java` (each constructor gains a `TripRepository` parameter, placed right after its own repository), `AppContext.java`, `TestData.java`
- Test: `service/TripServiceTest.java`; add cases to `service/TrainServiceTest.java`, `service/LineServiceTest.java`

**Produces**
```java
public class ScheduleConflictException extends MetroException {
    public ScheduleConflictException(Train train, List<Trip> conflicts)
    public List<Trip> getConflicts()
    // message, built from the first conflict:
    // "Train <code> is already on trip <id> (<HH:mm>–<HH:mm>, <line name>). Pick another train or time."
}

public class TripService {
    public static final int TURNAROUND_GAP_MINUTES = 5;
    public static final int MINUTES_PER_STOP = 3;

    public TripService(TripRepository trips, Session session, AuditLog audit, Clock clock)
    public List<Trip> listAll()                                  // VIEW_TRIPS; earliest departure first
    public List<Trip> find(String text, LocalDate from, LocalDate to, Line line, TripStatus status)
                                                                 // VIEW_TRIPS; any argument may be null = no filter;
                                                                 // text matches trip id, train code, line name, origin or
                                                                 // destination name, ignoring case;
                                                                 // dates compare the departure date, both ends included
    public LocalDateTime suggestArrival(Line line, LocalDateTime departure)   // VIEW_TRIPS
    public List<Trip> findConflicts(Train train, LocalDateTime departure,
                                    LocalDateTime arrival, String ignoreTripId)   // VIEW_TRIPS; ignoreTripId may be null
    public Trip create(Train train, Line line, Direction direction,
                       LocalDateTime departure, LocalDateTime arrival)        // MANAGE_TRIPS
    public Trip update(String id, Train train, Line line, Direction direction,
                       LocalDateTime departure, LocalDateTime arrival)        // MANAGE_TRIPS
    public Trip cancel(String id)                                // MANAGE_TRIPS
    public Trip complete(String id)                              // MANAGE_TRIPS
    public List<Trip> delay(String id, int minutes)              // MANAGE_TRIPS; returns the trips it now clashes with
    public int countUpcoming(Train train)                        // VIEW_TRIPS; Scheduled or Delayed, departing after now
}
```

**Rules and wording**
- `suggestArrival` = departure + (`line.size()` − 1) × `MINUTES_PER_STOP` minutes.
- `findConflicts` returns the train's other trips that are not Cancelled and for which `overlaps(…, TURNAROUND_GAP_MINUTES)` is true, earliest first.
- `create` and `update` refuse a train that is not Active → `Train <code> is <status display name> and cannot be scheduled`; then, if `findConflicts` is not empty, throw `ScheduleConflictException`.
- `update` on a Completed or Cancelled trip → `A <status display name> trip cannot be edited`.
- `delay`: minutes outside 1–240 → `Delay must be between 1 and 240 minutes`. Sets the status to Delayed and the delay to `minutes` (replacing any earlier delay), saves, and returns `findConflicts` for the new busy time. A delay is a fact, so it is saved even when it causes a clash; the caller shows the clash as a warning.
- `TrainService.update`: changing an Active train to In maintenance or Retired while `countUpcoming` > 0 → `Train <code> still has upcoming trips (<n>). Cancel or reassign them first.`
- `TrainService.delete`: any trip refers to the train → `Train <code> has trip history and cannot be deleted. Mark it Retired instead.`
- `LineService.delete`: any trip refers to the line → `<line name> has trips and cannot be deleted.`
- Audit actions: `TRIP_CREATED`, `TRIP_UPDATED`, `TRIP_CANCELLED`, `TRIP_COMPLETED`, `TRIP_DELAYED`; detail `<trip id> <line name> <HH:mm>`.

**Tests** — clock is 2026-11-15T10:00; "base" is a trip of train1 on Red, FORWARD, 12:00–12:45
```
TripServiceTest
  suggestedArrivalUsesStops:        suggestArrival(red, 12:00) equals 12:12        // 5 stations, 4 hops, 3 minutes each
  overlappingTripIsRefused:         create(train1, blue, FORWARD, 12:30, 13:00) throws ScheduleConflictException;
                                    message contains "Train T-101 is already on trip TRP-0001 (12:00–12:45, Red)";
                                    trips.findAll().size() stays 1
  insideTurnaroundGapIsRefused:     create(train1, red, BACKWARD, 12:48, 13:30) throws ScheduleConflictException
  afterTheGapIsAccepted:            create(train1, red, BACKWARD, 12:50, 13:30) succeeds
  anotherTrainIsNotAConflict:       create(train2, red, FORWARD, 12:00, 12:45) succeeds
  cancelledTripDoesNotBlock:        cancel(base); create(train1, red, FORWARD, 12:10, 12:55) succeeds
  editingATripDoesNotClashWithItself: update(base.id, train1, red, FORWARD, 12:05, 12:50) succeeds
  editIntoAnotherTripIsRefused:     with a second trip 14:00–14:45, update(base.id, …, 14:10, 14:50) throws; base keeps 12:00–12:45
  maintenanceTrainCannotBeScheduled: create(train3, …) -> "Train T-103 is In maintenance and cannot be scheduled"
  delayIsSavedAndReportsKnockOn:    second trip of train1 at 12:55–13:40; delay(base.id, 10) returns a list containing it;
                                    base is DELAYED with delay 10; trips.txt shows "DELAYED|10" for base
  delayWithoutKnockOnReturnsEmpty
  delayRange:                       delay(base.id, 0) and (…, 241) -> "Delay must be between 1 and 240 minutes"
  finishedTripCannotBeEdited:       complete(base.id); update(…) -> "A Completed trip cannot be edited"
  cancelThenCompleteIsRefused:      "A Cancelled trip cannot become Completed"
  findFiltersByDateLineAndStatus:   trips on the 14th, 15th, 16th across Red and Blue:
                                    find(null, 15th, 15th, null, null) only the 15th's; find(null, null, null, blue, null) only Blue's;
                                    find(null, 14th, 16th, red, CANCELLED) only the cancelled Red one;
                                    find("t-102", null, null, null, null) only train2's trips
  agentCanViewButNotCreate:         as agent listAll() works, create(…) throws AccessDeniedException
  controllerCanCreate
TrainServiceTest (added)
  trainWithUpcomingTripsCannotGoToMaintenance: update(train1.id, "T-101", 1200, MAINTENANCE)
                                    -> "Train T-101 still has upcoming trips (1). Cancel or reassign them first."
  trainWithHistoryCannotBeDeleted:  -> "Train T-101 has trip history and cannot be deleted. Mark it Retired instead."
  trainWithOnlyPastTripsCanRetire:  base completed and clock moved to the next day -> update to RETIRED succeeds
LineServiceTest (added)
  lineWithTripsCannotBeDeleted:     -> "Red has trips and cannot be deleted."
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add trip scheduling with conflict detection`

---

### Task 14: Demo trips and the Trips screen

**Files**
- Modify: `service/DemoData.java`, `AppContext.java` (seed trips on first run), `ui/MainWindow.java` (sidebar row under SCHEDULE)
- Create: `ui/TripsScreen.java`, `ui/TripFormDialog.java`
- Test: add cases to `service/DemoDataTest.java`

**Produces**
```java
// DemoData
public static void seedTrips(TripRepository trips, List<Line> lines, List<Train> activeTrains, LocalDate today)
```

**Demo trips**
- For each day from three days before `today` to three days after, each line gets six departures in each direction between 06:00 and 21:00, with the arrival from `MINUTES_PER_STOP`.
- Trains are assigned so that no train has two trips closer than `TURNAROUND_GAP_MINUTES` (keep, per train, the time it becomes free; pick the first free train).
- Trips before `today` are Completed, except one per past day that is Cancelled. On `today`, two trips are Delayed (10 and 15 minutes) and the rest Scheduled. Later days are all Scheduled.
- The result is the same every time for the same `today` (no unseeded randomness).

**Tests**
```
DemoDataTest (added)
  demoTripsCoverSevenDays:       departure dates span today-3 … today+3, every day present
  demoTripsHaveNoConflicts:      for every pair of non-cancelled trips of the same train,
                                 overlaps(…, TripService.TURNAROUND_GAP_MINUTES) is false
  pastTripsAreFinished:          every trip departing before today is COMPLETED or CANCELLED
  todayHasTwoDelayedTrips
  seedingIsRepeatable:           two fresh seeds with the same today give identical trips.txt content
```

**Trips screen** (`CrudScreen<Trip>`, add button `New trip`)
- Filters beside the search box: date (default today; a `Clear` link shows all dates), line, status. The search text and the filters are passed together to `TripService.find`.
- Columns: ID, Line (colour swatch and name), Route (`<origin> → <destination>`), Train, Departure (`dd MMM HH:mm`), Arrival (effective arrival; when delayed, followed by `+<n> min`), Status badge (Scheduled neutral, Delayed warning, Completed good, Cancelled bad).
- `canAdd()` and the row actions depend on `session.can(MANAGE_TRIPS)`. Without it the screen is a read-only list. With it the row actions are Edit, Delay, Complete, Cancel; Edit, Delay, Complete and Cancel are disabled for a trip whose status is final.
- `TripFormDialog`: line; direction shown as two choices `<first> → <last>` and `<last> → <first>`; train (from `TrainService.listActive()`); date; departure time (`HH:mm` text, invalid text → `Time must look like 14:05`); arrival time, filled from `suggestArrival` whenever line or departure changes and still editable. An arrival time earlier than the departure time means the next day. A `ScheduleConflictException` is shown inline and the dialog stays open.
- Delay asks for minutes. If `delay` returns clashes the screen shows a warning banner: `Delay saved. It now clashes with trip <id> — reschedule or cancel it.`
- Cancel asks `Cancel trip <id>?`.

- [ ] Write the demo-data tests · [ ] run, see them fail · [ ] implement `seedTrips` · [ ] run, see them pass
- [ ] Build the screen and dialog; add the sidebar row
- [ ] Screenshots run. Check `controller-trips.png` (buttons present, badges coloured, two delayed rows) and `agent-trips.png` (same list, no New trip button, no row actions).
- [ ] Click-through as `controller`:
  1. New trip on Line 1 for tomorrow 09:00 with `T-101` → arrival is pre-filled; Save → row appears; `data/trips.txt` gains a line.
  2. New trip, same train, tomorrow 09:10 → refused inline with the conflict sentence naming the first trip.
  3. Edit the first trip to 09:30 → saved. Delay it by 20 → badge Delayed, arrival shows `+20 min`.
  4. Complete it → Edit, Delay, Complete and Cancel are disabled for that row.
  5. Cancel another scheduled trip → badge Cancelled; filter status Cancelled shows it.
  6. Filter by a date with no trips → `Nothing to show yet.`
- [ ] As `admin`: Trains → set a train with upcoming trips to In maintenance → refused with the count.
- [ ] Full suite · [ ] Commit `Add trips screen and demo timetable`
- [ ] Push, pull request for `phase-3-schedule`, green check, merge.
