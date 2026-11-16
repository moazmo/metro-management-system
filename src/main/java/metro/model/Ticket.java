package metro.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import metro.exception.ValidationException;
import metro.util.Money;

/**
 * A ticket for one journey between two stations.
 *
 * This class is abstract: a real ticket is a StandardTicket, a StudentTicket
 * or a SeniorTicket. The subclasses differ in one thing only, their discount
 * (discountRate). The fare is worked out here, once, from the base fare and
 * whatever discount the subclass gives. Code that sells or reports tickets
 * calls ticket.getFare() and never needs to know which kind it holds. That
 * is polymorphism.
 *
 * A ticket keeps the number of stops and the base fare it was sold with, so
 * later changes to lines or fares never change an old ticket.
 *
 * The seller and the refunder are kept as user ids rather than User objects,
 * because a user's object is replaced when their role changes.
 */
public abstract class Ticket extends Entity {

    private final Station from;
    private final Station to;
    private final int stops;
    private final double baseFare;
    private final String soldById;
    private final LocalDateTime soldAt;
    private TicketStatus status = TicketStatus.VALID;
    private LocalDateTime usedAt;
    private String refundedById;
    private LocalDateTime refundedAt;

    protected Ticket(String id, Station from, Station to, int stops, double baseFare,
                     String soldById, LocalDateTime soldAt) {
        super(id);
        if (from == null || to == null || soldAt == null) {
            throw new IllegalArgumentException("A ticket needs two stations and a sale time");
        }
        this.from = from;
        this.to = to;
        this.stops = stops;
        this.baseFare = baseFare;
        this.soldById = soldById;
        this.soldAt = soldAt;
    }

    /** Which kind of ticket this is. */
    public abstract TicketType getType();

    /** The share of the base fare that is taken off: 0.0 is full price, 0.5 is half price. */
    public abstract double discountRate();

    /** What the passenger pays: the base fare less this ticket type's discount. */
    public double getFare() {
        return Money.round(baseFare * (1 - discountRate()));
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public int getStops() {
        return stops;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public String getSoldById() {
        return soldById;
    }

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public String getRefundedById() {
        return refundedById;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    /** Rule: a ticket is valid only on the day it was sold. */
    public boolean isExpired(LocalDate today) {
        return soldAt.toLocalDate().isBefore(today);
    }

    /** Rule: a ticket can be used once, and not after it was refunded. */
    public void markUsed(LocalDateTime when) {
        if (status == TicketStatus.USED) {
            throw new ValidationException("Ticket is already used");
        }
        if (status == TicketStatus.REFUNDED) {
            throw new ValidationException("Ticket was refunded");
        }
        status = TicketStatus.USED;
        usedAt = when;
    }

    /** Rule: only a ticket that was never used can be refunded, and only once. */
    public void refund(String byUserId, LocalDateTime when) {
        if (status == TicketStatus.USED) {
            throw new ValidationException("Ticket is already used");
        }
        if (status == TicketStatus.REFUNDED) {
            throw new ValidationException("Ticket is already refunded");
        }
        status = TicketStatus.REFUNDED;
        refundedById = byUserId;
        refundedAt = when;
    }

    /** Puts back the state a ticket had when it was saved. Only the repository calls this, when loading. */
    public void restore(TicketStatus status, LocalDateTime usedAt, String refundedById, LocalDateTime refundedAt) {
        this.status = status;
        this.usedAt = usedAt;
        this.refundedById = refundedById;
        this.refundedAt = refundedAt;
    }
}
