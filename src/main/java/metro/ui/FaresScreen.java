package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import metro.AppContext;
import metro.exception.ValidationException;
import metro.model.FareTier;
import metro.model.Journey;
import metro.model.Line;
import metro.model.TicketType;
import metro.service.FareService;
import metro.service.TicketService;
import metro.util.Money;

/**
 * Admins edit the fare table here: "up to so many stops costs so much".
 * Under the table the discount of each ticket type is shown.
 */
public class FaresScreen extends CrudScreen<FareTier> {

    private final FareService service;

    public FaresScreen(AppContext context) {
        super(context, "Fares", "Add tier");
        this.service = context.getFareService();
        setEmptyText("No fare tiers.");
        Label discounts = new Label(describeDiscounts(context));
        discounts.getStyleClass().add("text-muted");
        setFooter(discounts);
    }

    /**
     * Works out each ticket type's discount by asking the ticket service for
     * real quotes, so this text can never disagree with what is charged.
     */
    private String describeDiscounts(AppContext context) {
        List<Line> lines = context.getLineService().listAll();
        if (lines.isEmpty()) {
            return "";
        }
        TicketService tickets = context.getTicketService();
        Journey sample = tickets.previewJourney(lines.get(0).getFirstStation(), lines.get(0).getStations().get(1));
        double full = tickets.quoteFare(sample, TicketType.STANDARD);
        StringBuilder text = new StringBuilder("Ticket types:  ");
        for (TicketType type : TicketType.values()) {
            long percentOff = Math.round((1 - tickets.quoteFare(sample, type) / full) * 100);
            if (type != TicketType.STANDARD) {
                text.append("   ·   ");
            }
            text.append(type.getDisplayName()).append(": ")
                    .append(percentOff == 0 ? "full fare" : percentOff + "% off");
        }
        return text.toString();
    }

    @Override
    protected List<FareTier> loadItems(String searchText) {
        String wanted = searchText == null ? "" : searchText.trim();
        List<FareTier> shown = new ArrayList<>();
        for (FareTier tier : service.listTiers()) {
            if (wanted.isEmpty() || String.valueOf(tier.getMaxStops()).contains(wanted)
                    || Money.plain(tier.getPrice()).contains(wanted)) {
                shown.add(tier);
            }
        }
        return shown;
    }

    @Override
    protected void buildColumns(TableView<FareTier> table) {
        addColumn(table, "ID", tier -> tier.getId());
        addColumn(table, "Up to (stops)", tier -> String.valueOf(tier.getMaxStops()));
        addColumn(table, "Price", tier -> Money.format(tier.getPrice()));
    }

    @Override
    protected void openForm(FareTier existingOrNull) {
        if (showDialog(new TierDialog(service, existingOrNull))) {
            refresh();
            showInfo(existingOrNull == null ? "Tier added." : "Tier updated.");
        }
    }

    @Override
    protected void deleteItem(FareTier tier) {
        service.deleteTier(tier.getId());
    }

    @Override
    protected String deleteQuestion(FareTier tier) {
        return "Delete the tier for up to " + tier.getMaxStops() + " stops?";
    }

    /** The form to add or edit one tier. */
    private static class TierDialog extends FormDialog {
        private final FareService service;
        private final FareTier existing;
        private final Spinner<Integer> stops;
        private final TextField price;

        TierDialog(FareService service, FareTier existingOrNull) {
            super(existingOrNull == null ? "Add fare tier" : "Edit fare tier");
            this.service = service;
            this.existing = existingOrNull;
            stops = addNumber("Up to (stops)", 0, 999, existing == null ? 10 : existing.getMaxStops());
            price = addText("Price (EGP)", existing == null ? "" : Money.plain(existing.getPrice()));
        }

        @Override
        protected void save() {
            double amount;
            try {
                amount = Double.parseDouble(price.getText().trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Price must be a number, for example 8.50");
            }
            if (existing == null) {
                service.addTier(stops.getValue(), amount);
            } else {
                service.updateTier(existing.getId(), stops.getValue(), amount);
            }
        }
    }
}
