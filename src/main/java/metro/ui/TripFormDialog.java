package metro.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import metro.AppContext;
import metro.exception.ValidationException;
import metro.model.Direction;
import metro.model.Line;
import metro.model.Train;
import metro.model.Trip;
import metro.service.TripService;
import metro.util.Dates;

/**
 * The form to schedule a new trip or change an existing one (specification 2a, 2b).
 *
 * The arrival time is suggested from the length of the line whenever the
 * line or the departure changes, and can still be typed over. A schedule
 * conflict reported by the service is shown inside the form.
 */
public class TripFormDialog extends FormDialog {

    private final TripService service;
    private final Trip existing;
    private final ComboBox<Line> line;
    private final ComboBox<String> direction;
    private final ComboBox<Train> train;
    private final DatePicker date = new DatePicker();
    private final TextField departure;
    private final TextField arrival;

    public TripFormDialog(AppContext context, Trip existingOrNull) {
        super(existingOrNull == null ? "New trip" : "Edit trip " + existingOrNull.getId());
        this.service = context.getTripService();
        this.existing = existingOrNull;

        List<Line> lines = context.getLineService().listAll();
        List<Train> trains = new ArrayList<>(context.getTrainService().listActive());
        if (existing != null && !trains.contains(existing.getTrain())) {
            trains.add(existing.getTrain());
        }

        line = addChoice("Line", lines, existing == null ? lines.get(0) : existing.getLine());
        direction = addChoice("Direction", new ArrayList<>(), null);
        train = addChoice("Train", trains, existing == null ? trains.get(0) : existing.getTrain());
        date.setValue(existing == null
                ? LocalDate.now(context.getClock()).plusDays(1)
                : existing.getDeparture().toLocalDate());
        date.setMaxWidth(Double.MAX_VALUE);
        addRow("Date", date);
        departure = addText("Departure (HH:mm)", existing == null ? "09:00" : Dates.time(existing.getDeparture()));
        arrival = addText("Arrival (HH:mm)", existing == null ? "" : Dates.time(existing.getArrival()));

        fillDirections();
        if (existing != null && existing.getDirection() == Direction.BACKWARD) {
            direction.getSelectionModel().select(1);
        }
        if (existing == null) {
            suggestArrival();
        }
        line.setOnAction(event -> {
            fillDirections();
            suggestArrival();
        });
        departure.textProperty().addListener((observable, before, after) -> suggestArrival());
    }

    /** The two directions are shown as "first station → last station" and the reverse. */
    private void fillDirections() {
        Line chosen = line.getValue();
        int selected = Math.max(0, direction.getSelectionModel().getSelectedIndex());
        direction.getItems().setAll(
                chosen.getFirstStation().getName() + " → " + chosen.getLastStation().getName(),
                chosen.getLastStation().getName() + " → " + chosen.getFirstStation().getName());
        direction.getSelectionModel().select(selected);
    }

    /** Fills the arrival field from the service's suggestion when the departure is a valid time. */
    private void suggestArrival() {
        try {
            LocalDateTime start = LocalDate.of(2000, 1, 1).atTime(parseTime(departure.getText()));
            arrival.setText(Dates.time(service.suggestArrival(line.getValue(), start)));
        } catch (ValidationException e) {
            // the departure is still being typed; leave the arrival as it is
        }
    }

    private LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text == null ? "" : text.trim(), Dates.TIME);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Time must look like 14:05");
        }
    }

    @Override
    protected void save() {
        if (date.getValue() == null) {
            throw new ValidationException("Date is required");
        }
        LocalDateTime start = date.getValue().atTime(parseTime(departure.getText()));
        LocalDateTime end = date.getValue().atTime(parseTime(arrival.getText()));
        if (!end.isAfter(start)) {
            end = end.plusDays(1); // an arrival "earlier" than the departure means the next day
        }
        Direction way = direction.getSelectionModel().getSelectedIndex() == 1
                ? Direction.BACKWARD : Direction.FORWARD;
        if (existing == null) {
            service.create(train.getValue(), line.getValue(), way, start, end);
        } else {
            service.update(existing.getId(), train.getValue(), line.getValue(), way, start, end);
        }
    }
}
