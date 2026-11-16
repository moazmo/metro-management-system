package metro.ui;

import java.util.Arrays;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import metro.model.Station;
import metro.model.StationStatus;
import metro.service.StationService;

/** The form to add or edit a station. The status can be changed only when editing. */
public class StationFormDialog extends FormDialog {

    private final StationService service;
    private final Station existing;
    private final TextField name;
    private final TextField nameAr;
    private ComboBox<StationStatus> status;

    public StationFormDialog(StationService service, Station existingOrNull) {
        super(existingOrNull == null ? "Add station" : "Edit " + existingOrNull.getName());
        this.service = service;
        this.existing = existingOrNull;
        name = addText("Name", existing == null ? "" : existing.getName());
        nameAr = addText("Arabic name", existing == null ? "" : existing.getNameAr());
        if (existing != null) {
            status = addChoice("Status", Arrays.asList(StationStatus.values()), existing.getStatus());
        }
    }

    @Override
    protected void save() {
        if (existing == null) {
            service.create(name.getText(), nameAr.getText());
        } else {
            service.update(existing.getId(), name.getText(), nameAr.getText(), status.getValue());
        }
    }
}
