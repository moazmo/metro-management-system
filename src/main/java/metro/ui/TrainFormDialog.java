package metro.ui;

import java.util.Arrays;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import metro.model.Train;
import metro.model.TrainStatus;
import metro.service.TrainService;

/** The form to add or edit a train. The status can be changed only when editing. */
public class TrainFormDialog extends FormDialog {

    private final TrainService service;
    private final Train existing;
    private final TextField code;
    private final Spinner<Integer> capacity;
    private ComboBox<TrainStatus> status;

    public TrainFormDialog(TrainService service, Train existingOrNull) {
        super(existingOrNull == null ? "Add train" : "Edit " + existingOrNull.getCode());
        this.service = service;
        this.existing = existingOrNull;
        code = addText("Train ID", existing == null ? "" : existing.getCode());
        // The spinner allows 0 so that the service, not the control, explains the allowed range.
        capacity = addNumber("Capacity", 0, 9999, existing == null ? 1800 : existing.getCapacity());
        if (existing != null) {
            status = addChoice("Status", Arrays.asList(TrainStatus.values()), existing.getStatus());
        }
    }

    @Override
    protected void save() {
        if (existing == null) {
            service.create(code.getText(), capacity.getValue());
        } else {
            service.update(existing.getId(), code.getText(), capacity.getValue(), status.getValue());
        }
    }
}
