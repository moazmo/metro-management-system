package metro.service.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import metro.model.Ticket;
import metro.model.TicketStatus;
import metro.util.Money;

/**
 * Specification 4b: the number of tickets sold for each journey.
 *
 * A journey is a pair "from station, to station"; A to G and G to A are two
 * different journeys. Refunded tickets are left out, because a refunded
 * sale was undone.
 */
public class TicketReport extends Report {

    private final List<JourneyCount> journeys = new ArrayList<>();

    /** The busiest journey first; equal counts are ordered by the station names. */
    private static class BusiestFirst implements Comparator<JourneyCount> {
        @Override
        public int compare(JourneyCount a, JourneyCount b) {
            if (a.getTickets() != b.getTickets()) {
                return b.getTickets() - a.getTickets();
            }
            int byFrom = a.getFrom().getName().compareTo(b.getFrom().getName());
            if (byFrom != 0) {
                return byFrom;
            }
            return a.getTo().getName().compareTo(b.getTo().getName());
        }
    }

    public TicketReport(List<Ticket> allTickets, LocalDate from, LocalDate to) {
        super("Ticket report", from, to);
        for (Ticket ticket : allTickets) {
            if (inRange(ticket.getSoldAt()) && ticket.getStatus() != TicketStatus.REFUNDED) {
                countFor(ticket).add(ticket.getFare());
            }
        }
        journeys.sort(new BusiestFirst());
    }

    /** The row for the ticket's journey; a new row is started when there is none yet. */
    private JourneyCount countFor(Ticket ticket) {
        for (JourneyCount journey : journeys) {
            if (journey.getFrom().equals(ticket.getFrom()) && journey.getTo().equals(ticket.getTo())) {
                return journey;
            }
        }
        JourneyCount fresh = new JourneyCount(ticket.getFrom(), ticket.getTo());
        journeys.add(fresh);
        return fresh;
    }

    public List<JourneyCount> getJourneys() {
        return new ArrayList<>(journeys);
    }

    public int getTotalTickets() {
        int total = 0;
        for (JourneyCount journey : journeys) {
            total += journey.getTickets();
        }
        return total;
    }

    @Override
    public List<String> getColumns() {
        return Arrays.asList("From", "To", "Tickets", "Revenue");
    }

    @Override
    public List<String[]> getRows() {
        List<String[]> rows = new ArrayList<>();
        for (JourneyCount journey : journeys) {
            rows.add(new String[] {
                journey.getFrom().getName(),
                journey.getTo().getName(),
                String.valueOf(journey.getTickets()),
                Money.plain(journey.getRevenue())
            });
        }
        return rows;
    }
}
