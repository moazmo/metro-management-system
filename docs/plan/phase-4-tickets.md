# Phase 4 — Tickets (Tasks 15–19)

Part of the [build plan](../plan.md). Covers spec lines 3a, 3b, 3c, 3d.
Branch: `phase-4-tickets`.

Result: staff sell tickets between any two stations, the fare is worked
out from the shortest route, tickets are validated once and only on
their day of sale, and eligible tickets are refunded.

---

### Task 15: Route planner

**Files**
- Create: `model/Leg.java`, `model/Journey.java`, `service/RoutePlanner.java`
- Modify: `AppContext.java`, `TestData.java`
- Test: `service/RoutePlannerTest.java`; add one case to `service/DemoDataTest.java`

**Consumes:** `Line`, `LineRepository`, the fixture network in `TestData` (Task 8).

**Produces**
```java
public class Leg {                       // a stretch travelled on one line
    public Leg(Line line, Station from, Station to, int stops)
    public Line getLine(); public Station getFrom(); public Station getTo(); public int getStops();
}

public class Journey {
    public Journey(List<Station> stations, List<Leg> legs)
    public List<Station> getStations()   // origin first, destination last
    public Station getFrom(); public Station getTo();
    public int getStops()                // stations.size() - 1
    public List<Leg> getLegs()
    public int getTransfers()            // legs.size() - 1
}

public class RoutePlanner {
    public RoutePlanner(LineRepository lines)
    public Journey plan(Station from, Station to)
}
```

**Rules and wording**
- The network is a graph: two stations are neighbours when they are next to each other on any line. `plan` uses breadth-first search, so the journey has the fewest stops. Neighbours are visited in a fixed order (lines in id order; on each line the earlier station before the later one) so the same question always gives the same answer.
- Legs: walk the found path pair by pair. Stay on the current line while it has the next pair adjacent; otherwise start a new leg on the first line (in id order) that has the pair adjacent.
- `from` equals `to` → `Origin and destination must be different`.
- No path → `No route between <from name> and <to name>`.
- The planner reads the lines each time it is asked, so an edited network is used immediately.
- The planner has no permission check of its own; the services that call it do.

**Tests** — fixture network: Red A B C D E · Blue F C G H · Green H E I · Island X Y
```
RoutePlannerTest
  sameLine:                  plan(A, E): stops 4, 1 leg (Red A->E, 4 stops), transfers 0
  oneChange:                 plan(A, G): stations [A, B, C, G]; legs Red A->C (2), Blue C->G (1); transfers 1
  picksTheShorterOfTwoWays:  plan(D, H): stations [D, E, H]; stops 2; legs Red D->E (1), Green E->H (1)
  reverseHasTheSameLength:   plan(H, D).getStops() == 2
  twoChanges:                plan(F, I): stops 4; transfers 2; legs on Blue, Red, Green in that order
  sameStationIsRejected:     plan(A, A) -> ValidationException "Origin and destination must be different"
  noRouteIsAPlainMessage:    plan(A, X) -> ValidationException "No route between A and X"
  editedNetworkIsUsed:       after adding station Y to the end of Red through LineService, plan(A, X).getStops() == 6
DemoDataTest (added)
  demoNetworkIsFullyConnected: plan(Helwan, s) succeeds for every other station s
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add shortest-route planner`

---

### Task 16: Fare tiers

**Files**
- Create: `model/FareTier.java`, `repository/FareTierRepository.java`, `service/FarePolicy.java`, `service/TieredFarePolicy.java`, `service/FareService.java`
- Modify: `service/DemoData.java`, `AppContext.java`, `TestData.java`
- Test: `service/TieredFarePolicyTest.java`, `service/FareServiceTest.java`

**Produces**
```java
public class FareTier extends Entity {
    public FareTier(String id, int maxStops, double price)
    public int getMaxStops(); public void setMaxStops(int maxStops)   // Check.range(maxStops, 1, 200, "Stops")
    public double getPrice(); public void setPrice(double price)      // Check.positive(price, "Price"), then Money.round
}

public interface FarePolicy { double baseFare(int stops); }

public class TieredFarePolicy implements FarePolicy {
    public TieredFarePolicy(List<FareTier> tiers)    // at least one tier, else IllegalArgumentException
    public double baseFare(int stops)                // stops < 1 -> IllegalArgumentException
}

public class FareTierRepository extends FileRepository<FareTier>     // "Fare tier", "FAR", 2 digits, header "id|maxStops|price"

public class FareService {
    public FareService(FareTierRepository tiers, Session session, AuditLog audit)
    public List<FareTier> listTiers()                // any logged-in user; lowest maxStops first
    public FarePolicy currentPolicy()                // any logged-in user
    public FareTier addTier(int maxStops, double price)                  // MANAGE_FARES
    public FareTier updateTier(String id, int maxStops, double price)    // MANAGE_FARES
    public void deleteTier(String id)                // MANAGE_FARES
}

// DemoData
public static void seedFares(FareTierRepository tiers)
```
"Any logged-in user" means the method starts with `session.requireLogin()` (Task 5).

**Rules and wording**
- `baseFare` returns the price of the lowest tier whose `maxStops` is at least `stops`; beyond the highest tier it returns the highest tier's price.
- Two tiers with the same `maxStops` → `A tier for up to <n> stops already exists`.
- Deleting the only tier → `At least one fare tier is required`.
- Demo tiers (example values, admin-editable; check against current official fares before the demo): up to 9 stops EGP 8.00, up to 16 EGP 10.00, up to 23 EGP 15.00, up to 39 EGP 20.00.
- Audit actions: `FARE_TIER_ADDED`, `FARE_TIER_UPDATED`, `FARE_TIER_DELETED`.

**Tests** — with the demo tiers
```
TieredFarePolicyTest
  tierBoundaries:   baseFare(1)=8, (9)=8, (10)=10, (16)=10, (17)=15, (23)=15, (24)=20, (39)=20
  beyondTheLastTierUsesTheLastPrice: baseFare(60) == 20
  tiersNeedNotBeSorted: the same answers when the list is given in reverse order
  zeroStopsIsAProgrammingError: assertThrows(IllegalArgumentException.class, () -> policy.baseFare(0))
FareServiceTest
  agentCanReadButNotChange:  as agent listTiers() works; addTier(5, 6) throws AccessDeniedException
  duplicateTierIsRefused:    addTier(9, 7) -> "A tier for up to 9 stops already exists"
  lastTierCannotBeDeleted
  priceMustBePositive:       addTier(5, 0) -> "Price must be greater than zero"
  changeIsUsedImmediately:   updateTier(first.id, 9, 9.5) -> currentPolicy().baseFare(3) == 9.5; fares.txt shows 9.5
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add editable fare tiers`

---

### Task 17: Tickets

**Files**
- Create: `model/TicketType.java`, `model/TicketStatus.java`, `model/Ticket.java`, `model/StandardTicket.java`, `model/StudentTicket.java`, `model/SeniorTicket.java`, `model/TicketFactory.java`, `repository/TicketRepository.java`
- Modify: `AppContext.java`, `TestData.java`
- Test: `model/TicketTest.java`, `repository/TicketRepositoryTest.java`

**Produces**
```java
public enum TicketType   { STANDARD("Standard"), STUDENT("Student"), SENIOR("Senior") }   // getDisplayName()
public enum TicketStatus { VALID("Valid"), USED("Used"), REFUNDED("Refunded") }            // getDisplayName()

public abstract class Ticket extends Entity {
    protected Ticket(String id, Station from, Station to, int stops, double baseFare,
                     String soldById, LocalDateTime soldAt)             // status VALID
    public abstract TicketType getType();
    public abstract double discountRate();           // 0.0 = full price, 0.5 = half price
    public double getFare()                          // Money.round(baseFare * (1 - discountRate()))
    public Station getFrom(); public Station getTo(); public int getStops(); public double getBaseFare();
    public String getSoldById(); public LocalDateTime getSoldAt();
    public TicketStatus getStatus();
    public LocalDateTime getUsedAt();                // null unless USED
    public String getRefundedById(); public LocalDateTime getRefundedAt();   // null unless REFUNDED
    public boolean isExpired(LocalDate today)        // sold on a day before today
    public void markUsed(LocalDateTime when)
    public void refund(String byUserId, LocalDateTime when)
    public void restore(TicketStatus status, LocalDateTime usedAt, String refundedById, LocalDateTime refundedAt)
                                                     // only for the repository, when loading
}

public class StandardTicket extends Ticket           // discountRate 0.0
public class StudentTicket  extends Ticket           // discountRate 0.50
public class SeniorTicket   extends Ticket           // discountRate 0.30

public final class TicketFactory {
    public static Ticket create(TicketType type, String id, Station from, Station to, int stops,
                                double baseFare, String soldById, LocalDateTime soldAt)
}

public class TicketRepository extends FileRepository<Ticket> {
    public TicketRepository(Path file, StationRepository stations)
    // "Ticket", "TKT", 6 digits,
    // header "id|type|fromId|toId|stops|baseFare|soldById|soldAt|status|usedAt|refundedById|refundedAt"
    public boolean existsForStation(Station station)
}
```
The seller and the refunder are stored as user ids, not `User` objects, because a role change replaces the user's object (Task 5).

**Rules and wording**
- `markUsed` on a ticket that is not Valid → `Ticket is already used` or `Ticket was refunded`.
- `refund` on a ticket that is not Valid → `Ticket is already used` or `Ticket is already refunded`.
- A ticket keeps the stops and base fare it was sold with. Later changes to lines or fare tiers do not change old tickets.
- A ticket file naming an unknown station → `tickets.txt line <n>: unknown station <id>`.

**Tests**
```
TicketTest
  eachTypeHasItsOwnFare:       base 10.00 -> Standard 10.00, Student 5.00, Senior 7.00   (called through a Ticket variable)
  fareIsRoundedToTwoDecimals:  base 15.00 Senior -> 10.50; base 8.25 Student -> 4.13
  newTicketIsValid
  usedOnlyOnce:                markUsed(t); markUsed(t) -> "Ticket is already used"; getUsedAt() is the first time
  usedTicketCannotBeRefunded:  -> "Ticket is already used"
  refundedTicketCannotBeUsed:  -> "Ticket was refunded"
  refundRecordsWhoAndWhen
  expiresAtMidnight:           sold 2026-11-15T23:59: isExpired(2026-11-15) false; isExpired(2026-11-16) true
  factoryBuildsTheRightClass:  for each TicketType, create(type, …).getType() == type
TicketRepositoryTest
  typeAndStatusSurviveReload:  a used StudentTicket and a refunded SeniorTicket come back with the same class,
                               status, times and refunder id
  fileLineLooksRight:          "TKT-000001|STUDENT|STN-001|STN-007|3|8.0|USR-003|2026-11-15T10:00:00|VALID|||"
  unknownStationNamesFileAndLine
  existsForStation:            true for a ticket's from and to stations, false for another station
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add ticket types with their own fares`

---

### Task 18: Selling, validating and refunding

**Files**
- Create: `model/TicketCheck.java`, `service/TicketService.java`
- Modify: `service/StationService.java` (constructor gains a `TicketRepository` parameter after `LineRepository`), `AppContext.java`, `TestData.java`
- Test: `service/TicketServiceTest.java`; add cases to `service/StationServiceTest.java`

**Consumes:** `RoutePlanner`, `Journey` (Task 15); `FareService` (Task 16); `Ticket`, `TicketFactory`, `TicketRepository` (Task 17); `MutableClock` (Task 5).

**Produces**
```java
public class TicketCheck {
    public TicketCheck(boolean accepted, String message, Ticket ticket)
    public boolean isAccepted(); public String getMessage();
    public Ticket getTicket()                    // null when the ticket was not found
}

public class TicketService {
    public static final int MAX_QUANTITY = 20;

    public TicketService(TicketRepository tickets, RoutePlanner planner, FareService fares,
                         Session session, AuditLog audit, Clock clock)
    public Journey previewJourney(Station from, Station to)          // VIEW_NETWORK; route only, for the map
    public Journey planJourney(Station from, Station to)             // SELL_TICKETS; also refuses closed stations
    public double quoteFare(Journey journey, TicketType type)        // any logged-in user; the fare of one ticket
    public List<Ticket> sell(Station from, Station to, TicketType type, int quantity)   // SELL_TICKETS
    public List<Ticket> mySalesToday()                               // SELL_TICKETS; newest first
    public TicketCheck inspect(String ticketId)                      // VALIDATE_TICKETS; changes nothing
    public TicketCheck validate(String ticketId)                     // VALIDATE_TICKETS; marks the ticket Used when accepted
    public TicketCheck refundCheck(String ticketId)                  // REFUND_TICKETS; accepted means eligible
    public Ticket refund(String ticketId)                            // REFUND_TICKETS
}
```

**Rules and wording**
- `previewJourney` is `RoutePlanner.plan` behind a permission check.
- `planJourney` and `sell`: a closed origin or destination → `<station name> is closed`; then `RoutePlanner.plan`.
- `quoteFare` builds a throw-away ticket of that type with `TicketFactory` (never saved) and returns its `getFare()`, so a quote and a sale can never disagree.
- `sell`: quantity outside 1–`MAX_QUANTITY` → `Quantity must be between 1 and 20`. Creates one ticket per unit, each with its own id from `nextId()`, the journey's stops, the base fare from `fares.currentPolicy()`, the logged-in user's id and the current time.
- Ticket ids typed by a person are trimmed and upper-cased before lookup.
- `inspect` and `validate` messages, in this order of checks:

  | Condition | accepted | Message |
  |---|---|---|
  | no such ticket | false | `Ticket not found` |
  | Used | false | `Already used at <HH:mm> on <dd MMM yyyy>` |
  | Refunded | false | `Refunded — not valid for travel` |
  | sold before today | false | `Expired — sold on <dd MMM yyyy>` |
  | otherwise, `inspect` | true | `Valid` |
  | otherwise, `validate` | true | `Valid — marked as used` |

- `refundCheck` messages, in this order:

  | Condition | accepted | Message |
  |---|---|---|
  | no such ticket | false | `Ticket not found` |
  | Used | false | `Already used — cannot be refunded` |
  | Refunded | false | `Already refunded` |
  | sold before today | false | `Sold on <dd MMM yyyy> — refunds are same-day only` |
  | otherwise | true | `Eligible for a refund of <Money.format(fare)>` |

- `refund` runs `refundCheck`; when not eligible it throws `ValidationException` with that message; otherwise it refunds the full fare, saves and returns the ticket.
- `StationService.delete`: a ticket refers to the station → `<name> has ticket history and cannot be deleted. Mark it Closed instead.` (checked after the on-a-line rule).
- Audit actions: `TICKET_SOLD` (one entry per sale, detail `<first id>…<last id> <from> → <to> x<quantity>`), `TICKET_USED`, `TICKET_REJECTED` (detail includes the message), `TICKET_REFUNDED`.

**Tests** — clock starts at 2026-11-15T10:00; fixture network; demo fare tiers; logged in as agent unless stated
```
TicketServiceTest
  sellCreatesOneRecordPerTicket:   sell(A, G, STUDENT, 3) -> 3 tickets with consecutive ids, stops 3, base fare 8.00,
                                   fare 4.00, soldById == agent.getId(); tickets.txt has 3 record lines
  quoteMatchesWhatIsSold:          quoteFare(planJourney(A, G), STUDENT) == 4.00
  closedStationIsRefused:          G set to CLOSED -> sell(A, G, STANDARD, 1) throws "G is closed"
  noRouteIsRefused:                sell(A, X, STANDARD, 1) throws "No route between A and X"; no ticket saved
  quantityLimits:                  0 and 21 -> "Quantity must be between 1 and 20"
  controllerCanPreviewButNotSell:  as controller previewJourney(A, G).getStops() == 3 and
                                   quoteFare(that journey, STANDARD) == 8.00; sell(…) throws AccessDeniedException
  validTicketIsAcceptedOnce:       validate(id) accepted "Valid — marked as used"; ticket USED, usedAt 10:00;
                                   validate(id) again not accepted, "Already used at 10:00 on 15 Nov 2026"
  inspectChangesNothing:           inspect(id) accepted "Valid"; ticket still VALID
  idIsFoundDespiteCaseAndSpaces:   inspect("  tkt-000001 ") accepted
  unknownTicket:                   inspect("TKT-999999") not accepted, "Ticket not found", getTicket() null
  ticketExpiresAtMidnight:         sold at 23:59; clock set to 2026-11-16T00:01;
                                   validate -> "Expired — sold on 15 Nov 2026"; ticket still VALID
  agentCannotRefund:               refund(id) as agent -> AccessDeniedException
  supervisorRefundsSameDay:        as supervisor: refundCheck accepted "Eligible for a refund of EGP 4.00";
                                   refund(id) -> REFUNDED, refundedById == supervisor.getId()
  refundedTicketIsNotValid:        then validate(id) -> "Refunded — not valid for travel"
  refundTwiceIsRefused:            -> ValidationException "Already refunded"
  usedTicketIsNotRefundable:       -> ValidationException "Already used — cannot be refunded"
  yesterdaysTicketIsNotRefundable: clock moved to next day -> "Sold on 15 Nov 2026 — refunds are same-day only"
  oldTicketsSurviveANetworkEdit:   sell(A, G, STANDARD, 1); remove G from Blue through LineService; reload every repository
                                   -> the ticket loads, to == G, stops 3, fare 8.00
  mySalesTodayShowsOnlyMine:       agent sells 2, supervisor sells 1 -> as agent mySalesToday().size() == 2
StationServiceTest (added)
  stationWithTicketsCannotBeDeleted: after the network edit above, delete(G.id)
                                   -> "G has ticket history and cannot be deleted. Mark it Closed instead."
```

- [ ] Write the tests · [ ] run, see them fail · [ ] implement · [ ] run, see them pass · [ ] full suite
- [ ] Commit `Add ticket sales, validation and refunds`

---

### Task 19: Ticket screens, fares screen, demo tickets

**Files**
- Create: `ui/SellTicketScreen.java`, `ui/TicketCard.java`, `ui/ValidateTicketScreen.java`, `ui/RefundsScreen.java`, `ui/FaresScreen.java`, `ui/FareTierFormDialog.java`
- Modify: `service/DemoData.java`, `AppContext.java` (seed fares and tickets on first run), `ui/MainWindow.java` (four sidebar rows under TICKETS)
- Test: add cases to `service/DemoDataTest.java`

**Produces**
```java
// DemoData
public static void seedTickets(TicketRepository tickets, List<Station> openStations, RoutePlanner planner,
                               FarePolicy policy, List<User> sellers, LocalDate today)

public class TicketCard extends VBox { public TicketCard(Ticket ticket) }
```

**Demo tickets**
- For each of the seven days ending `today`: between 40 and 80 tickets between random pairs of stations, sold between 06:00 and 22:00 by the demo agent and supervisor; roughly 70% Standard, 20% Student, 10% Senior. Uses `new Random(42)` so the result is repeatable.
- Tickets of past days: about 90% Used (a few minutes to two hours after sale), about 3% Refunded (same day, by the supervisor), the rest left Valid (they are expired). Today's tickets: about half Used, the rest Valid.

**Tests**
```
DemoDataTest (added)
  everyDayHasSales:                each of the 7 days has between 40 and 80 tickets
  faresMatchThePolicy:             for every ticket, baseFare == policy.baseFare(stops)
  refundsAreSameDay:               every REFUNDED ticket's refundedAt date equals its soldAt date
  usedAfterSold:                   every USED ticket's usedAt is after its soldAt and on the same day
  ticketSeedingIsRepeatable
```

**Screens**
- **Sell ticket** (`Screen`). Left: From and To (searchable choices of open stations), ticket type (three toggle buttons), quantity (1–20), `Sell` button. Centre: a live summary that updates on every change — one line per leg (`<line name>: <from> → <to> (<n> stops)`), `Change at <station>` between legs, then stops, changes, fare per ticket, total. While the selection is incomplete or invalid the summary shows the reason and `Sell` is disabled. Right: after a sale, one `TicketCard` per ticket; under it "My sales today" (code, journey, fare, status).
- `TicketCard`: looks like a printed ticket — the code large and monospaced, `<from> → <to>`, type, fare, sold date and time, `Valid on <dd MMM yyyy> only`.
- **Validate** (`Screen`): one large code field (Enter = Check), buttons `Check` (calls `inspect`) and `Validate` (calls `validate`). A verdict panel: green with a tick when accepted, red with a cross when not, the message in large text, and the ticket's details when it exists. After Validate the field is cleared and focused for the next ticket.
- **Refunds** (`Screen`): code field and `Find`; shows the ticket details and the `refundCheck` message; `Refund` is enabled only when eligible and asks `Refund <amount> for ticket <id>?`.
- **Fares** (`CrudScreen<FareTier>`, add button `Add tier`): columns `Up to (stops)`, `Price`. Under the table a read-only box: `Standard: full fare · Student: 50% off · Senior: 30% off`, built from the three ticket classes' `discountRate()`.

- [ ] Write the demo-data tests · [ ] run, see them fail · [ ] implement `seedFares` wiring and `seedTickets` · [ ] run, see them pass
- [ ] Build the four screens and add the sidebar rows
- [ ] Screenshots run. Check `agent-sell-ticket.png`, `agent-validate.png`, `supervisor-refunds.png`, `admin-fares.png`; and that `agent-refunds.png`, `agent-fares.png` and `controller-sell-ticket.png` do not exist.
- [ ] Click-through as `agent`:
  1. Sell ticket: Helwan → a station on Line 2. The summary shows two legs with `Change at Sadat` (or the interchange the planner picked), the stops and the fare. Switch type to Student → the fare halves. Quantity 2 → Sell → two ticket cards; `data/tickets.txt` gains two lines.
  2. Validate: paste the first code → Check says `Valid`; Validate says `Valid — marked as used`; Validate again says `Already used at …`. The line in `tickets.txt` shows `USED`.
  3. Type nonsense → `Ticket not found`.
- [ ] Click-through as `supervisor`: Refunds → the second code → eligible → Refund → confirmed; Find again → `Already refunded`. Try yesterday's demo ticket that is still Valid → `Sold on … — refunds are same-day only`.
- [ ] Click-through as `admin`: Fares → change the first tier's price → Sell ticket shows the new fare for a short journey; change it back.
- [ ] Full suite · [ ] Commit `Add ticket screens and demo sales`
- [ ] Push, pull request for `phase-4-tickets`, green check, merge.
