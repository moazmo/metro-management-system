# Schedule — explain-guide

Owner: member 4. Trips: creating, changing and cancelling them, their status,
and detecting conflicting schedules.

## 1. What this module does

Specification module 2:

- 2a — *"Trip Scheduling: Authorized users can create train schedules with departure and arrival times."*
- 2b — *"Schedule Modification: Users can update or cancel scheduled trips."*
- 2c — *"Service Status: The system should track trains as scheduled, delayed, cancelled, or completed."*
- 2d — *"Schedule Validation: The system should detect conflicting schedules for the same train."*

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `Trip` | model | One run of a train along a line, with times, status and delay |
| `TripStatus` | model | Scheduled, Delayed, Completed, Cancelled, and which changes are allowed |
| `Direction` | model | Forward (first → last station) or Backward |
| `TripRepository` | repository | `trips.txt`; trips of a train or of a line |
| `TripService` | service | Scheduling rules and the conflict detector |
| `ScheduleConflictException` | exception | A trip clashes with another trip of the same train; carries the clashing trips |
| `TripsScreen` | ui | Timetable with date, line and status filters; buttons only for controllers and admins |
| `TripFormDialog` | ui | New or edit trip; arrival suggested from the line length |

## 3. OOP concepts in this module

**Encapsulation** — [Trip.java](../../src/main/java/metro/model/Trip.java).
The status has no plain setter. The only way to change it is
`changeStatus(next)`, which refuses a change `TripStatus` does not allow, so a
completed trip can never be "scheduled" again.

**Inheritance** — `Trip` extends `Entity`; `TripRepository` extends `FileRepository`;
`TripsScreen` extends `CrudScreen`; `TripFormDialog` extends `FormDialog`;
`ScheduleConflictException` extends `MetroException`.

**Polymorphism** — `TripsScreen` overrides `rowActions(...)` and `canAdd()`
from `CrudScreen`: the same screen class gives a controller buttons and an agent
a read-only list. `ScheduleConflictException` is caught by `FormDialog` as a
plain `MetroException`, and its message is shown like any other.

**Abstraction** — `TripStatus` keeps the status rules in one place:
`canChangeTo` and `isFinal`. Nothing else needs to know the table of allowed changes.

**Relationships** — a `Trip` is *associated* with one `Train` and one `Line`: it
uses them but does not own them. Many trips share one train.

## 4. The data file

`data/trips.txt`:

```
#next=337
# id|trainId|lineId|direction|departure|arrival|status|delayMinutes
TRP-0001|TRN-001|LIN-01|FORWARD|2026-11-13T06:00:00|2026-11-13T07:42:00|COMPLETED|0
```

| Field | Meaning |
|---|---|
| `TRP-0001` | trip id |
| `TRN-001` | the train (by its fixed id) |
| `LIN-01` | the line |
| `FORWARD` | from the line's first station (Helwan) to its last (New El-Marg) |
| `…T06:00:00` | departure |
| `…T07:42:00` | planned arrival: 34 stops × 3 minutes |
| `COMPLETED` | status |
| `0` | delay in minutes; the real arrival is the planned one plus this |

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| Arrival must be after departure | `Trip.reschedule` | `TripTest.arrivalMustBeAfterDeparture` |
| Only allowed status changes | `TripStatus.canChangeTo`, `Trip.changeStatus` | `TripTest.finalStatusesCannotChange`, `finishedTripCannotBeReopened` |
| Two trips of one train must not overlap, with a 5-minute gap | `TripService.findConflicts`, `Trip.overlaps` | `TripServiceTest.overlappingTripIsRefused`, `insideTurnaroundGapIsRefused`, `afterTheGapIsAccepted` |
| Another train is never a conflict | `TripService.findConflicts` | `TripServiceTest.anotherTrainIsNotAConflict` |
| Cancelled trips do not block | `TripService.findConflicts` | `TripServiceTest.cancelledTripDoesNotBlock` |
| A trip does not clash with itself when edited | `findConflicts(…, ignoreTripId)` | `TripServiceTest.editingATripDoesNotClashWithItself` |
| A refused edit leaves the trip as it was | `TripService.update` | `TripServiceTest.editIntoAnotherTripIsRefused` |
| Only active trains can be scheduled | `TripService.check` | `TripServiceTest.maintenanceTrainCannotBeScheduled` |
| A delay moves the arrival and warns about knock-on clashes | `TripService.delay` | `TripServiceTest.delayIsSavedAndReportsKnockOn` |
| A trip across midnight is handled | `Trip.overlaps` | `TripTest.tripAcrossMidnightWorks`, `TripServiceTest.tripAcrossMidnightIsScheduledAndChecked` |
| Finished trips cannot be edited | `TripService.update` | `TripServiceTest.finishedTripCannotBeEdited` |
| Agents can view but not change | `session.require(MANAGE_TRIPS)` | `TripServiceTest.agentCanViewButNotCreate` |
| Filters combine | `TripService.find` | `TripServiceTest.findFiltersByDateLineStatusAndText` |

## 6. One action from click to file: scheduling a trip that clashes

1. `TripsScreen` → **New trip** opens `TripFormDialog`. Choosing a line fills the arrival with `tripService.suggestArrival` (stops × 3 minutes).
2. **Save** calls `TripFormDialog.save()`: it reads the date and the two times (an arrival "earlier" than the departure means the next day), then `tripService.create(train, line, direction, start, end)`.
3. `TripService.create` checks `MANAGE_TRIPS`, then `check(...)`: the train must be active; a trial `Trip` checks arrival after departure; `findConflicts` looks at every other trip of that train.
4. `findConflicts` skips cancelled trips and asks `other.overlaps(start, end, 5)`: two time ranges overlap when each one starts before the other one ends (plus the 5-minute turnaround gap).
5. A clash throws `ScheduleConflictException` with the clashing trips. Nothing has been saved.
6. `FormDialog` catches it and shows *Train T-101 is already on trip TRP-… (06:00–07:42, Line 1). Pick another train or time.* The form stays open.
7. With a free time, `create` builds the `Trip`, `trips.save(trip)` rewrites `trips.txt`, and `TRIP_CREATED` goes to the audit log.

## 7. Questions you may be asked

1. **How do you detect a conflicting schedule?** For the same train, two trips conflict when their time ranges overlap: trip A starts before B ends and B starts before A ends. We add a 5-minute turnaround gap. Cancelled trips do not count.
2. **Why the turnaround gap?** A train arriving at 10:45 cannot leave on another trip at 10:45; it needs a few minutes.
3. **What happens when a trip is delayed?** Its status becomes Delayed and its real arrival moves. The delay is always saved — a delay is a fact — and if the train now clashes with its next trip, the screen warns so the controller can fix it.
4. **Why can't a completed trip be edited?** History must stay true. `TripStatus.isFinal()` is true for Completed and Cancelled.
5. **Where are the allowed status changes written?** Only in `TripStatus.canChangeTo`. `Trip.changeStatus` uses it, so no class can skip it.
6. **What does "Direction" change?** Which end the trip starts from: `getOrigin()` is the first station for Forward and the last for Backward.
7. **How is a trip across midnight handled?** Times are full date-times, not only hours, so 23:50 → 00:20 the next day compares correctly.
8. **What does an agent see?** The same Trips list, without the New trip button and without row buttons: `canAdd()` and `rowActions()` check `MANAGE_TRIPS`. The service checks again.
9. **Why does `ScheduleConflictException` carry the trips?** So the message can name the clashing trip, and so other code could show all of them.
10. **What is the association between Trip and Train?** A trip refers to one train; a train runs many trips. The trip does not own the train.
11. **How was the demo timetable built without conflicts?** For each train we remember when it is free again; each trip takes the first free train. A test checks every pair of trips of the same train.
12. *Change it:* **make the turnaround gap 10 minutes.** Change `TripService.TURNAROUND_GAP_MINUTES`; `afterTheGapIsAccepted` then fails because 12:50 is now too early.

## 8. Try it yourself

1. Add a "Platform" number to `Trip`: field, constructor, `TripRepository`, form and table column.
2. Add a rule: a trip may not be scheduled more than 30 days ahead, with a test.
3. In `trips.txt`, find a delayed trip of today and check that its arrival plus delay matches the Trips screen.
