package metro.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import metro.model.Permission;
import metro.model.Ticket;
import metro.model.TicketStatus;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.repository.TicketRepository;
import metro.repository.TripRepository;
import metro.util.Money;

/**
 * The live numbers on the dashboard. Each number has the permission of the
 * screen it summarises, so a role only sees the figures it is allowed to see
 * elsewhere (for example, only report viewers see today's revenue).
 */
public class DashboardService {

    private final TripRepository trips;
    private final TicketRepository tickets;
    private final Session session;
    private final Clock clock;

    public DashboardService(TripRepository trips, TicketRepository tickets, Session session, Clock clock) {
        this.trips = trips;
        this.tickets = tickets;
        this.session = session;
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDate today() {
        return now().toLocalDate();
    }

    /** Trips departing today that are not cancelled. */
    public int tripsToday() {
        session.require(Permission.VIEW_TRIPS);
        int count = 0;
        for (Trip trip : trips.findAll()) {
            if (trip.getDeparture().toLocalDate().equals(today()) && trip.getStatus() != TripStatus.CANCELLED) {
                count++;
            }
        }
        return count;
    }

    public int delayedToday() {
        session.require(Permission.VIEW_TRIPS);
        int count = 0;
        for (Trip trip : trips.findAll()) {
            if (trip.getDeparture().toLocalDate().equals(today()) && trip.getStatus() == TripStatus.DELAYED) {
                count++;
            }
        }
        return count;
    }

    /** The next trips still to run (scheduled or delayed), the earliest first. */
    public List<Trip> nextDepartures(int limit) {
        session.require(Permission.VIEW_TRIPS);
        List<Trip> coming = new ArrayList<>();
        for (Trip trip : trips.findAll()) {
            if (!trip.getStatus().isFinal() && !trip.getDeparture().isBefore(now())) {
                coming.add(trip);
            }
        }
        coming.sort(new Comparator<Trip>() {
            @Override
            public int compare(Trip a, Trip b) {
                return a.getDeparture().compareTo(b.getDeparture());
            }
        });
        List<Trip> first = new ArrayList<>();
        for (int i = 0; i < coming.size() && i < limit; i++) {
            first.add(coming.get(i));
        }
        return first;
    }

    public int ticketsSoldToday() {
        session.require(Permission.VIEW_REPORTS);
        int count = 0;
        for (Ticket ticket : tickets.findAll()) {
            if (ticket.getSoldAt().toLocalDate().equals(today())) {
                count++;
            }
        }
        return count;
    }

    /** Today's sales minus today's refunds. */
    public double revenueToday() {
        session.require(Permission.VIEW_REPORTS);
        double total = 0;
        for (Ticket ticket : tickets.findAll()) {
            if (ticket.getSoldAt().toLocalDate().equals(today())) {
                total += ticket.getFare();
            }
            if (ticket.getStatus() == TicketStatus.REFUNDED && ticket.getRefundedAt().toLocalDate().equals(today())) {
                total -= ticket.getFare();
            }
        }
        return Money.round(total);
    }

    /** Tickets the logged-in user sold today. */
    public int mySalesToday() {
        session.require(Permission.SELL_TICKETS);
        int count = 0;
        for (Ticket ticket : tickets.findAll()) {
            if (ticket.getSoldAt().toLocalDate().equals(today())
                    && ticket.getSoldById().equals(session.getUser().getId())) {
                count++;
            }
        }
        return count;
    }
}
