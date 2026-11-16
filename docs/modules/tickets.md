# Tickets — explain-guide

Owner: member 5. Ticket sales, fares, validation and refunds.

## 1. What this module does

Specification module 3:

- 3a — *"Ticket Sales: Staff can record ticket sales for selected stations and journeys."*
- 3b — *"Fare Calculation: The system should calculate the fare based on the selected journey or number of stations."*
- 3c — *"Ticket Validation: The system should check ticket details and mark a ticket as used when appropriate."*
- 3d — *"Ticket Cancellation: Authorized staff can record eligible ticket cancellations and refunds."*

## 2. Classes

| Class | Layer | What it is for |
|---|---|---|
| `Ticket` | model | Abstract ticket: journey, base fare, sale, status; works out the fare |
| `StandardTicket` | model | Full price |
| `StudentTicket` | model | 50% off |
| `SeniorTicket` | model | 30% off |
| `TicketType` | model | Standard, Student, Senior |
| `TicketStatus` | model | Valid, Used, Refunded |
| `TicketFactory` | model | Creates the right ticket subclass for a type |
| `TicketCheck` | model | The answer of a check: accepted or not, message, ticket |
| `FareTier` | model | "Up to N stops costs X" |
| `FarePolicy` | service | Interface: base fare for a number of stops |
| `TieredFarePolicy` | service | The fare policy built from the tiers |
| `FareService` | service | Read the fare table (everyone), change it (admins) |
| `TicketService` | service | Selling, quoting, validating, refunding |
| `TicketRepository` | repository | `tickets.txt` |
| `FareTierRepository` | repository | `fares.txt` |
| `SellTicketScreen` | ui | Choose a journey, see route and fare, sell; ticket cards; my sales today |
| `ValidateTicketScreen` | ui | Check or validate a code; green or red verdict |
| `RefundsScreen` | ui | Find a ticket, see if it is eligible, record the refund |
| `FaresScreen` | ui | Edit the fare tiers; shows each type's discount |
| `TicketCard` | ui | A sold ticket drawn like a printed ticket |
| `TicketDetails` | ui | A small table of a ticket's facts |

## 3. OOP concepts in this module

**Polymorphism** — [Ticket.java](../../src/main/java/metro/model/Ticket.java)
and [StudentTicket.java](../../src/main/java/metro/model/StudentTicket.java).
`getFare()` is written once in `Ticket`: base fare × (1 − `discountRate()`).
`discountRate()` is abstract and each subclass answers differently (0, 0.5, 0.3).
Selling, reports and the dashboard call `ticket.getFare()` and never ask which
type it is. Adding a Child ticket needs a new subclass and nothing else changes.

**Abstraction** — `Ticket` is abstract (a ticket is always one of the types).
[FarePolicy.java](../../src/main/java/metro/service/FarePolicy.java) is an
interface: the ticket service asks "what is the base fare for 22 stops?" and does
not care how it is decided.

**Inheritance** — three ticket classes extend `Ticket`; `Ticket` extends `Entity`.
`TicketRepository` and `FareTierRepository` extend `FileRepository`.

**Encapsulation** — the stops, base fare, seller and sale time are `final`. A
ticket keeps the price it was sold at, even if fares change later. The status
changes only through `markUsed` and `refund`, which enforce the rules.

**Relationships** — a ticket is *associated* with two `Station`s (from, to). The
`TicketService` *uses* the `RoutePlanner` and the `FareService` (dependency).

## 4. The data file

`data/tickets.txt`:

```
# id|type|fromId|toId|stops|baseFare|soldById|soldAt|status|usedAt|refundedById|refundedAt
TKT-000001|STANDARD|STN-046|STN-058|14|10.0|USR-004|2026-11-10T06:06:00|USED|2026-11-10T06:29:00||
```

| Field | Meaning |
|---|---|
| `TKT-000001` | the code printed on the ticket |
| `STANDARD` | the type; decides which subclass is created when loading |
| `STN-046` → `STN-058` | from and to stations |
| `14` | stops of the shortest route, fixed at sale time |
| `10.0` | base fare from the tier for 14 stops; the passenger paid this less the type's discount |
| `USR-004` | who sold it |
| `…06:06:00` | when it was sold; it is valid on that day only |
| `USED` / `…06:29:00` | status and when it was used |
| (empty) (empty) | who refunded it and when — only for refunded tickets |

`data/fares.txt`: `FAR-01|9|8.0` — up to 9 stops costs EGP 8.00.

## 5. Rules and where they live

| Rule | Method | Test |
|---|---|---|
| Each type has its own fare | `Ticket.getFare`, `discountRate` | `TicketTest.eachTypeHasItsOwnFare` |
| Fares are rounded to two decimals | `Ticket.getFare` | `TicketTest.fareIsRoundedToTwoDecimals` |
| Fare from the tier of the number of stops | `TieredFarePolicy.baseFare` | `FareServiceTest.tierBoundaries` |
| A quote always equals the sale | `TicketService.quoteFare` | `TicketServiceTest.quoteMatchesWhatIsSold` |
| One record per ticket sold | `TicketService.sell` | `TicketServiceTest.sellCreatesOneRecordPerTicket` |
| No tickets to or from a closed station | `TicketService.planJourney` | `TicketServiceTest.closedStationIsRefused` |
| Quantity is 1 to 20 | `TicketService.sell` | `TicketServiceTest.quantityLimits` |
| A ticket is used only once | `Ticket.markUsed`, `TicketService.validate` | `TicketServiceTest.validTicketIsAcceptedOnce` |
| A ticket is valid only on its day of sale | `Ticket.isExpired` | `TicketServiceTest.ticketExpiresAtMidnight` |
| Refunds: unused, same day, full fare, once | `TicketService.refundCheck`, `refund` | `TicketServiceTest.supervisorRefundsSameDay`, `usedTicketIsNotRefundable`, `yesterdaysTicketIsNotRefundable`, `refundTwiceIsRefused` |
| Agents cannot refund | `session.require(REFUND_TICKETS)` | `TicketServiceTest.agentCannotRefund` |
| Old tickets keep their journey and price | `Ticket` final fields | `TicketServiceTest.oldTicketsSurviveANetworkEdit` |
| At least one fare tier always exists | `FareService.deleteTier` | `FareServiceTest.lastTierCannotBeDeleted` |

## 6. One action from click to file: selling a ticket

1. `SellTicketScreen`: choose From Helwan and To Cairo University. Each change calls `updateSummary()`.
2. `updateSummary` calls `ticketService.planJourney(from, to)`: checks `SELL_TICKETS`, both stations open, then `RoutePlanner.plan` — 22 stops, legs Line 1 to Sadat and Line 2 to Cairo University.
3. It calls `quoteFare(journey, type)`: the tier for 22 stops is EGP 15.00; a trial ticket of the chosen type is asked for `getFare()`. The summary shows legs, *Change at Sadat* and the total.
4. **Sell** calls `ticketService.sell(from, to, type, quantity)`. It checks the quantity, plans again, takes the base fare, and creates one ticket per unit with `TicketFactory`, each with a new `TKT-` id.
5. `tickets.saveAll(sold)` adds them and rewrites `tickets.txt` once.
6. One `TICKET_SOLD` line goes to `audit.log`; the screen shows a `TicketCard` per ticket.

## 7. Questions you may be asked

1. **How is the fare calculated?** The route planner finds the route with the fewest stops; the fare table gives the base fare for that number of stops; the ticket type takes off its discount.
2. **Where is polymorphism here?** `getFare()` calls `discountRate()`, and each ticket class answers it differently. Code holding a `Ticket` variable gets the right fare without checking the type.
3. **Why not one Ticket class with a `type` field and an if?** Then every new type means editing an if in several places. With subclasses, a new type is one new class.
4. **Why is `FarePolicy` an interface?** So another pricing scheme (flat fare, zones) could be added as a new class without touching the ticket service.
5. **Why does a ticket store its stops and base fare?** So later changes to lines or fares never change what an old ticket says it cost. Reports stay true.
6. **What makes a ticket invalid at the gate?** Not found, already used, refunded, or sold on an earlier day. `TicketCheck` carries the exact reason.
7. **What is the difference between Check and Validate?** Check only looks (`inspect`). Validate also marks an acceptable ticket as used and saves it.
8. **Who can refund, and when?** Station supervisors and admins; only a ticket never used and sold today; the full fare.
9. **Is a refunded ticket deleted?** No. It keeps its record with status Refunded, who refunded it and when. The revenue report subtracts it on the refund day.
10. **How does the quote match the sale exactly?** `quoteFare` builds a throw-away ticket of that type and asks it for `getFare()` — the same method a real sale uses.
11. **What if a user types the code in small letters?** Codes are trimmed and turned to capitals before the lookup.
12. *Change it:* **add a Child ticket at 75% off.** A `ChildTicket` subclass with `discountRate()` 0.75, a `CHILD` value in `TicketType`, a case in `TicketFactory`. The sell screen's buttons and the Fares note pick it up by themselves.

## 8. Try it yourself

1. Add the Child ticket above and sell one.
2. Make refunds allowed until 2 hours after sale instead of the same day, and update the tests.
3. Sell a ticket, then find its line in `tickets.txt` and explain each field.
