package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import metro.AppContext;
import metro.model.Line;
import metro.model.Station;
import metro.model.StationStatus;
import metro.service.StationService;

/**
 * Specification 1a: admins add, update, delete, list and search stations.
 */
public class StationsScreen extends CrudScreen<Station> {

    private static final String ALL = "All statuses";

    private final StationService service;
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public StationsScreen(AppContext context) {
        super(context, "Stations", "Add station");
        this.service = context.getStationService();
        setEmptyText("No stations match.");

        statusFilter.getItems().add(ALL);
        for (StationStatus status : StationStatus.values()) {
            statusFilter.getItems().add(status.getDisplayName());
        }
        statusFilter.setValue(ALL);
        statusFilter.setOnAction(event -> refresh());
        addFilter(statusFilter);
    }

    @Override
    protected List<Station> loadItems(String searchText) {
        List<Station> shown = new ArrayList<>();
        for (Station station : service.search(searchText)) {
            String wanted = statusFilter.getValue();
            if (wanted.equals(ALL) || station.getStatus().getDisplayName().equals(wanted)) {
                shown.add(station);
            }
        }
        return shown;
    }

    @Override
    protected void buildColumns(TableView<Station> table) {
        addColumn(table, "ID", station -> station.getId());
        addColumn(table, "Name", station -> station.getName());
        addColumn(table, "Arabic name", station -> station.getNameAr());
        addColumn(table, "Lines", station -> lineNames(station));
        addColumn(table, "Interchange", station -> service.isInterchange(station) ? "Yes" : "");
        addBadgeColumn(table, "Status", station -> station.isOpen()
                ? new StatusBadge("Open", StatusBadge.Tone.GOOD)
                : new StatusBadge("Closed", StatusBadge.Tone.WARNING));
    }

    private String lineNames(Station station) {
        StringBuilder names = new StringBuilder();
        for (Line line : service.linesOf(station)) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(line.getName());
        }
        return names.toString();
    }

    @Override
    protected void openForm(Station existingOrNull) {
        if (showDialog(new StationFormDialog(service, existingOrNull))) {
            refresh();
            showInfo(existingOrNull == null ? "Station added." : "Station updated.");
        }
    }

    @Override
    protected void deleteItem(Station station) {
        service.delete(station.getId());
    }

    @Override
    protected String deleteQuestion(Station station) {
        return "Delete station " + station.getName() + "?";
    }
}
