package metro.repository;

import java.nio.file.Path;
import metro.model.Station;
import metro.model.Ticket;
import metro.model.TicketFactory;
import metro.model.TicketStatus;
import metro.model.TicketType;
import metro.util.Dates;

/**
 * Stores tickets in tickets.txt. A ticket is saved with the ids of its two
 * stations, so stations must be loaded before tickets.
 */
public class TicketRepository extends FileRepository<Ticket> {

    private final StationRepository stations;

    public TicketRepository(Path file, StationRepository stations) {
        super(file, "Ticket", "TKT", 6,
                "id|type|fromId|toId|stops|baseFare|soldById|soldAt|status|usedAt|refundedById|refundedAt");
        this.stations = stations;
    }

    @Override
    protected Ticket parse(String[] fields) {
        Ticket ticket = TicketFactory.create(
                TicketType.valueOf(fields[1]), fields[0], station(fields[2]), station(fields[3]),
                Integer.parseInt(fields[4]), Double.parseDouble(fields[5]), fields[6], Dates.parse(fields[7]));
        String refundedBy = fields[10].isEmpty() ? null : fields[10];
        ticket.restore(TicketStatus.valueOf(fields[8]), Dates.parse(fields[9]), refundedBy, Dates.parse(fields[11]));
        return ticket;
    }

    private Station station(String id) {
        Station station = stations.findById(id);
        if (station == null) {
            throw new IllegalArgumentException("unknown station " + id);
        }
        return station;
    }

    @Override
    protected String[] format(Ticket ticket) {
        return new String[] {
            ticket.getId(), ticket.getType().name(), ticket.getFrom().getId(), ticket.getTo().getId(),
            String.valueOf(ticket.getStops()), String.valueOf(ticket.getBaseFare()),
            ticket.getSoldById(), Dates.file(ticket.getSoldAt()), ticket.getStatus().name(),
            Dates.file(ticket.getUsedAt()), ticket.getRefundedById(), Dates.file(ticket.getRefundedAt())
        };
    }

    /** True when some ticket starts or ends at the station. */
    public boolean existsForStation(Station station) {
        for (Ticket ticket : findAll()) {
            if (ticket.getFrom().equals(station) || ticket.getTo().equals(station)) {
                return true;
            }
        }
        return false;
    }
}
