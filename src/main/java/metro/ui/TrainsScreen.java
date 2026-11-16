package metro.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import metro.AppContext;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.service.TrainService;

/**
 * Specification 1c: admins record and update train IDs, capacities and service status.
 */
public class TrainsScreen extends CrudScreen<Train> {

    private static final String ALL = "All statuses";

    private final TrainService service;
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public TrainsScreen(AppContext context) {
        super(context, "Trains", "Add train");
        this.service = context.getTrainService();
        setEmptyText("No trains match.");

        statusFilter.getItems().add(ALL);
        for (TrainStatus status : TrainStatus.values()) {
            statusFilter.getItems().add(status.getDisplayName());
        }
        statusFilter.setValue(ALL);
        statusFilter.setOnAction(event -> refresh());
        addFilter(statusFilter);
    }

    @Override
    protected List<Train> loadItems(String searchText) {
        List<Train> shown = new ArrayList<>();
        for (Train train : service.search(searchText)) {
            String wanted = statusFilter.getValue();
            if (wanted.equals(ALL) || train.getStatus().getDisplayName().equals(wanted)) {
                shown.add(train);
            }
        }
        return shown;
    }

    @Override
    protected void buildColumns(TableView<Train> table) {
        addColumn(table, "ID", train -> train.getId());
        addColumn(table, "Train ID", train -> train.getCode());
        addColumn(table, "Capacity", train -> String.valueOf(train.getCapacity()));
        addBadgeColumn(table, "Status", train -> badge(train.getStatus()));
    }

    /** The badge colour for each train status. */
    static StatusBadge badge(TrainStatus status) {
        switch (status) {
            case ACTIVE:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.GOOD);
            case MAINTENANCE:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.WARNING);
            default:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.NEUTRAL);
        }
    }

    @Override
    protected void openForm(Train existingOrNull) {
        if (showDialog(new TrainFormDialog(service, existingOrNull))) {
            refresh();
            showInfo(existingOrNull == null ? "Train added." : "Train updated.");
        }
    }

    @Override
    protected void deleteItem(Train train) {
        service.delete(train.getId());
    }

    @Override
    protected String deleteQuestion(Train train) {
        return "Delete train " + train.getCode() + "?";
    }
}
