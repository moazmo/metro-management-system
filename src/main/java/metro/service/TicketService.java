package metro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import metro.exception.ValidationException;
import metro.model.Journey;
import metro.model.Permission;
import metro.model.Station;
import metro.model.Ticket;
import metro.model.TicketCheck;
import metro.model.TicketFactory;
import metro.model.TicketStatus;
import metro.model.TicketType;
import metro.repository.AuditLog;
import metro.repository.TicketRepository;
import metro.util.Check;
import metro.util.Dates;
import metro.util.Money;

/**
 * The rules for tickets (specification module 3): selling, working out the
 * fare, validating a ticket once, and refunding eligible tickets.
 */
public class TicketService {

    public static final int MAX_QUANTITY = 20;

    private final TicketRepository tickets;
    private final RoutePlanner planner;
    private final FareService fares;
    private final Session session;
    private final AuditLog audit;
    private final Clock clock;

    public TicketService(TicketRepository tickets, RoutePlanner planner, FareService fares,
                         Session session, AuditLog audit, Clock clock) {
        this.tickets = tickets;
        this.planner = planner;
        this.fares = fares;
        this.session = session;
        this.audit = audit;
        this.clock = clock;
    }

    // ----- journeys and fares -----

    /** The route between two stations, for looking at the map. Nothing is sold. */
    public Journey previewJourney(Station from, Station to) {
        session.require(Permission.VIEW_NETWORK);
        return planner.plan(from, to);
    }

    /**
     * The route for a ticket.
     * Rule: no ticket is sold to or from a closed station.
     */
    public Journey planJourney(Station from, Station to) {
        session.require(Permission.SELL_TICKETS);
        if (!from.isOpen()) {
            throw new ValidationException(from.getName() + " is closed");
        }
        if (!to.isOpen()) {
            throw new ValidationException(to.getName() + " is closed");
        }
        return planner.plan(from, to);
    }

    /**
     * The fare of one ticket of the given type for a journey (specification 3b).
     *
     * A trial ticket of that type is built and asked for its fare, and then
     * thrown away. Because a real sale asks a ticket the same question, a
     * quote and a sale can never disagree.
     */
    public double quoteFare(Journey journey, TicketType type) {
        session.requireLogin();
        double baseFare = fares.currentPolicy().baseFare(journey.getStops());
        Ticket trial = TicketFactory.create(type, "QUOTE", journey.getFrom(), journey.getTo(),
                journey.getStops(), baseFare, "", now());
        return trial.getFare();
    }

    // ----- selling (3a) -----

    /** Sells one or more tickets for a journey. Each ticket is its own record with its own code. */
    public List<Ticket> sell(Station from, Station to, TicketType type, int quantity) {
        session.require(Permission.SELL_TICKETS);
        Check.range(quantity, 1, MAX_QUANTITY, "Quantity");
        if (type == null) {
            throw new ValidationException("Ticket type is required");
        }
        Journey journey = planJourney(from, to);
        double baseFare = fares.currentPolicy().baseFare(journey.getStops());
        LocalDateTime soldAt = now();

        List<Ticket> sold = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            sold.add(TicketFactory.create(type, tickets.nextId(), from, to, journey.getStops(),
                    baseFare, session.getUser().getId(), soldAt));
        }
        tickets.saveAll(sold);
        record("TICKET_SOLD", sold.get(0).getId() + "…" + sold.get(sold.size() - 1).getId()
                + " " + from.getName() + " → " + to.getName() + " x" + quantity);
        return sold;
    }

    /** The tickets the logged-in user sold today, the newest first. */
    public List<Ticket> mySalesToday() {
        session.require(Permission.SELL_TICKETS);
        LocalDate today = now().toLocalDate();
        List<Ticket> mine = new ArrayList<>();
        for (Ticket ticket : tickets.findAll()) {
            if (ticket.getSoldById().equals(session.getUser().getId())
                    && ticket.getSoldAt().toLocalDate().equals(today)) {
                mine.add(0, ticket);
            }
        }
        return mine;
    }

    // ----- validating (3c) -----

    /** Says whether a ticket would be accepted, without changing it. */
    public TicketCheck inspect(String ticketId) {
        session.require(Permission.VALIDATE_TICKETS);
        return checkForTravel(find(ticketId), "Valid");
    }

    /** Checks a ticket at the gate and, when it is acceptable, marks it as used. */
    public TicketCheck validate(String ticketId) {
        session.require(Permission.VALIDATE_TICKETS);
        TicketCheck check = checkForTravel(find(ticketId), "Valid — marked as used");
        if (check.isAccepted()) {
            check.getTicket().markUsed(now());
            tickets.save(check.getTicket());
            record("TICKET_USED", check.getTicket().getId());
        } else {
            record("TICKET_REJECTED", clean(ticketId) + ": " + check.getMessage());
        }
        return check;
    }

    /**
     * Rule: a ticket is accepted only when it exists, has not been used, has
     * not been refunded, and was sold today.
     */
    private TicketCheck checkForTravel(Ticket ticket, String acceptedMessage) {
        if (ticket == null) {
            return new TicketCheck(false, "Ticket not found", null);
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            return new TicketCheck(false, "Already used at " + Dates.time(ticket.getUsedAt())
                    + " on " + Dates.date(ticket.getUsedAt().toLocalDate()), ticket);
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            return new TicketCheck(false, "Refunded — not valid for travel", ticket);
        }
        if (ticket.isExpired(now().toLocalDate())) {
            return new TicketCheck(false, "Expired — sold on "
                    + Dates.date(ticket.getSoldAt().toLocalDate()), ticket);
        }
        return new TicketCheck(true, acceptedMessage, ticket);
    }

    // ----- refunding (3d) -----

    /**
     * Says whether a ticket may be refunded, and why not when it may not.
     * Rule: only a ticket that was never used and was sold today is refunded,
     * and the refund is the full fare paid.
     */
    public TicketCheck refundCheck(String ticketId) {
        session.require(Permission.REFUND_TICKETS);
        Ticket ticket = find(ticketId);
        if (ticket == null) {
            return new TicketCheck(false, "Ticket not found", null);
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            return new TicketCheck(false, "Already used — cannot be refunded", ticket);
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            return new TicketCheck(false, "Already refunded", ticket);
        }
        if (ticket.isExpired(now().toLocalDate())) {
            return new TicketCheck(false, "Sold on " + Dates.date(ticket.getSoldAt().toLocalDate())
                    + " — refunds are same-day only", ticket);
        }
        return new TicketCheck(true, "Eligible for a refund of " + Money.format(ticket.getFare()), ticket);
    }

    /** Records the refund of an eligible ticket. A ticket that is not eligible is refused with the reason. */
    public Ticket refund(String ticketId) {
        session.require(Permission.REFUND_TICKETS);
        TicketCheck check = refundCheck(ticketId);
        if (!check.isAccepted()) {
            throw new ValidationException(check.getMessage());
        }
        Ticket ticket = check.getTicket();
        ticket.refund(session.getUser().getId(), now());
        tickets.save(ticket);
        record("TICKET_REFUNDED", ticket.getId() + " " + Money.format(ticket.getFare()));
        return ticket;
    }

    // ----- helpers -----

    /** A ticket code typed by a person: spaces around it and small letters do not matter. */
    private String clean(String ticketId) {
        return ticketId == null ? "" : ticketId.trim().toUpperCase();
    }

    private Ticket find(String ticketId) {
        return tickets.findById(clean(ticketId));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private void record(String action, String detail) {
        audit.record(session.getUser().getUsername(), action, detail);
    }
}
