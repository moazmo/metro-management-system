package metro.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import metro.AppContext;
import metro.model.Line;
import metro.model.Permission;
import metro.model.Trip;
import metro.model.TripStatus;
import metro.service.TripService;
import metro.util.Dates;

/**
 * Specification module 2: the timetable.
 *
 * Everyone with "view trips" sees the list. Only users who may manage trips
 * see the buttons to create, edit, delay, complete and cancel them.
 */
public class TripsScreen extends CrudScreen<Trip> {

    private static final String ALL_LINES = "All lines";
    private static final String ALL_STATUSES = "All statuses";

    private final TripService service;
    private final boolean canManage;
    private final DatePicker dateFilter = new DatePicker();
    private final ComboBox<String> lineFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public TripsScreen(AppContext context) {
        super(context, "Trips", "New trip");
        this.service = context.getTripService();
        this.canManage = context.getSession().can(Permission.MANAGE_TRIPS);
        setEmptyText("No trips match these filters.");

        dateFilter.setValue(LocalDate.now(context.getClock()));
        dateFilter.setPrefWidth(140);
        dateFilter.setMinWidth(130);
        dateFilter.setOnAction(event -> refresh());
        Button allDates = new Button("All dates");
        allDates.getStyleClass().add("flat");
        allDates.setMinWidth(Region.USE_PREF_SIZE);
        allDates.setOnAction(event -> {
            dateFilter.setValue(null);
            refresh();
        });

        lineFilter.getItems().add(ALL_LINES);
        for (Line line : context.getLineService().listAll()) {
            lineFilter.getItems().add(line.getName());
        }
        lineFilter.setValue(ALL_LINES);
        lineFilter.setPrefWidth(150);
        lineFilter.setOnAction(event -> refresh());

        statusFilter.getItems().add(ALL_STATUSES);
        for (TripStatus status : TripStatus.values()) {
            statusFilter.getItems().add(status.getDisplayName());
        }
        statusFilter.setValue(ALL_STATUSES);
        statusFilter.setOnAction(event -> refresh());

        addFilter(dateFilter);
        addFilter(allDates);
        addFilter(lineFilter);
        addFilter(statusFilter);
    }

    @Override
    protected List<Trip> loadItems(String searchText) {
        Line line = null;
        for (Line candidate : context.getLineService().listAll()) {
            if (candidate.getName().equals(lineFilter.getValue())) {
                line = candidate;
            }
        }
        TripStatus status = null;
        for (TripStatus candidate : TripStatus.values()) {
            if (candidate.getDisplayName().equals(statusFilter.getValue())) {
                status = candidate;
            }
        }
        LocalDate day = dateFilter.getValue();
        return service.find(searchText, day, day, line, status);
    }

    @Override
    protected void buildColumns(TableView<Trip> table) {
        addColumn(table, "ID", trip -> trip.getId());
        addNodeColumn(table, "Line", trip -> new LineSwatch(trip.getLine()));
        addColumn(table, "Route", trip -> trip.getOrigin().getName() + " → " + trip.getDestination().getName());
        addColumn(table, "Train", trip -> trip.getTrain().getCode());
        addColumn(table, "Departure", trip -> trip.getDeparture().format(Dates.DATE_TIME));
        addColumn(table, "Arrival", trip -> arrivalText(trip));
        addBadgeColumn(table, "Status", trip -> badge(trip.getStatus()));
    }

    private String arrivalText(Trip trip) {
        String text = trip.getEffectiveArrival().format(Dates.DATE_TIME);
        if (trip.getDelayMinutes() > 0) {
            text += "  +" + trip.getDelayMinutes() + " min";
        }
        return text;
    }

    /** The badge colour for each trip status. */
    static StatusBadge badge(TripStatus status) {
        switch (status) {
            case DELAYED:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.WARNING);
            case COMPLETED:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.GOOD);
            case CANCELLED:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.BAD);
            default:
                return new StatusBadge(status.getDisplayName(), StatusBadge.Tone.NEUTRAL);
        }
    }

    @Override
    protected boolean canAdd() {
        return canManage;
    }

    @Override
    protected void openForm(Trip existingOrNull) {
        if (showDialog(new TripFormDialog(context, existingOrNull))) {
            refresh();
            showInfo(existingOrNull == null ? "Trip scheduled." : "Trip updated.");
        }
    }

    /** Without the permission there are no buttons at all; a finished trip cannot be changed. */
    @Override
    protected List<Button> rowActions(Trip selected) {
        List<Button> buttons = new ArrayList<>();
        if (!canManage) {
            return buttons;
        }
        buttons.add(action("Edit", "edit", () -> openForm(selected)));
        buttons.add(action("Delay", "more_time", () -> delay(selected)));
        buttons.add(action("Complete", "check_circle", () -> {
            service.complete(selected.getId());
            showInfo("Trip " + selected.getId() + " completed.");
        }));
        Button cancel = action("Cancel trip", "cancel", () -> {
            if (confirm("Cancel trip " + selected.getId() + "?")) {
                service.cancel(selected.getId());
                showInfo("Trip " + selected.getId() + " cancelled.");
            }
        });
        cancel.getStyleClass().add("danger");
        buttons.add(cancel);
        for (Button button : buttons) {
            button.setDisable(selected.getStatus().isFinal());
        }
        return buttons;
    }

    private void delay(Trip trip) {
        DelayDialog dialog = new DelayDialog(service, trip);
        if (showDialog(dialog)) {
            if (dialog.clashes.isEmpty()) {
                showInfo("Delay saved.");
            } else {
                showWarning("Delay saved. It now clashes with trip " + dialog.clashes.get(0).getId()
                        + " — reschedule or cancel it.");
            }
        }
    }

    /** Small form asking by how many minutes a trip is delayed. */
    private static class DelayDialog extends FormDialog {
        private final TripService service;
        private final Trip trip;
        private final Spinner<Integer> minutes;
        private List<Trip> clashes = new ArrayList<>();

        DelayDialog(TripService service, Trip trip) {
            super("Delay trip " + trip.getId());
            this.service = service;
            this.trip = trip;
            this.minutes = addNumber("Delay (minutes)", 0, 999, Math.max(10, trip.getDelayMinutes()));
        }

        @Override
        protected void save() {
            clashes = service.delay(trip.getId(), minutes.getValue());
        }
    }
}
