package metro.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import metro.AppContext;
import metro.exception.MetroException;
import metro.model.Identifiable;

/**
 * Base class of every screen that is a searchable list of records with
 * buttons to add, edit and delete (stations, trains, staff users...).
 *
 * The parts that are the same for all of them live here: the search box,
 * the table, the buttons and the refreshing. A subclass only says where the
 * records come from, which columns to show and which form to open.
 */
public abstract class CrudScreen<T extends Identifiable> extends Screen {

    private final TextField search = new TextField();
    private final HBox filters = new HBox(8);
    private final Button addButton;
    private final TableView<T> table = new TableView<>();
    private final HBox rowActionBar = new HBox(8);
    private final Label emptyText = new Label("Nothing to show yet.");
    private boolean built = false;

    protected CrudScreen(AppContext context, String title, String addButtonLabel) {
        super(context, title);

        search.setId("crud-search");
        table.setId("crud-table");
        search.setPromptText("Search…");
        search.setPrefWidth(260);
        search.textProperty().addListener((observable, before, after) -> refresh());
        filters.setAlignment(Pos.CENTER_LEFT);

        addButton = new Button(addButtonLabel, Icons.of("add", 16));
        addButton.getStyleClass().add("accent");
        addButton.setOnAction(event -> {
            clearMessage();
            openForm(null);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, search, filters, spacer, addButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        table.setPlaceholder(emptyText);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, before, after) -> showRowActions(after));
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !rowActionBar.getChildren().isEmpty()) {
                ((Button) rowActionBar.getChildren().get(0)).fire();
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        rowActionBar.setAlignment(Pos.CENTER_LEFT);
        rowActionBar.setMinHeight(36);

        setContent(new VBox(12, toolbar, table, rowActionBar));
    }

    // ----- what a subclass provides -----

    /** The records to show for the given search text. */
    protected abstract List<T> loadItems(String searchText);

    /** Adds the columns of the table, using addColumn and addBadgeColumn. */
    protected abstract void buildColumns(TableView<T> table);

    /** Opens the form to add (null) or edit a record, and calls refresh() after a save. */
    protected abstract void openForm(T existingOrNull);

    /** Deletes a record. Screens that allow deleting override this. */
    protected void deleteItem(T item) {
    }

    /** Whether the add button is shown. */
    protected boolean canAdd() {
        return true;
    }

    /** The buttons shown under the table for the selected record. */
    protected List<Button> rowActions(T selected) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(action("Edit", "edit", () -> openForm(selected)));
        Button delete = action("Delete", "delete", () -> {
            if (confirm(deleteQuestion(selected))) {
                deleteItem(selected);
                showInfo("Deleted.");
            }
        });
        delete.getStyleClass().add("danger");
        buttons.add(delete);
        return buttons;
    }

    /** The question asked before a record is deleted. */
    protected String deleteQuestion(T item) {
        return "Delete " + item.getId() + "?";
    }

    // ----- helpers for subclasses -----

    /** A text column. */
    protected final void addColumn(TableView<T> table, String heading, Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new SimpleStringProperty(text.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    /** A column that shows a coloured status badge. */
    protected final void addBadgeColumn(TableView<T> table, String heading, Function<T, StatusBadge> badge) {
        addNodeColumn(table, heading, item -> badge.apply(item));
    }

    /** A column that shows any small control, for example a colour swatch with a name. */
    protected final void addNodeColumn(TableView<T> table, String heading, Function<T, Node> node) {
        TableColumn<T, T> column = new TableColumn<>(heading);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        column.setCellFactory(col -> new TableCell<T, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : node.apply(item));
            }
        });
        column.setSortable(false);
        table.getColumns().add(column);
    }

    /** Places an extra filter control beside the search box. Call refresh() from its listener. */
    protected final void addFilter(Node control) {
        filters.getChildren().add(control);
    }

    /**
     * A button for the row-action bar. Its handler runs inside run(...), so a
     * refused action becomes a message, and the table is refreshed afterwards.
     */
    protected final Button action(String label, String icon, Runnable handler) {
        Button button = new Button(label, Icons.of(icon, 16));
        button.setOnAction(event -> {
            run(handler);
            refresh();
        });
        return button;
    }

    protected final void setEmptyText(String text) {
        emptyText.setText(text);
    }

    protected final T getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }

    protected final String getSearchText() {
        return search.getText();
    }

    /** Reloads the table, keeping the same record selected when it is still there. */
    protected final void refresh() {
        if (!built) {
            return;
        }
        T selected = getSelected();
        try {
            table.getItems().setAll(loadItems(search.getText()));
        } catch (MetroException e) {
            table.getItems().clear();
            showError(e.getMessage());
        }
        table.getSelectionModel().clearSelection();
        if (selected != null) {
            for (T item : table.getItems()) {
                if (item.getId().equals(selected.getId())) {
                    table.getSelectionModel().select(item);
                }
            }
        }
        table.refresh();
        showRowActions(getSelected());
    }

    private void showRowActions(T selected) {
        if (selected == null) {
            rowActionBar.getChildren().clear();
        } else {
            rowActionBar.getChildren().setAll(rowActions(selected));
        }
    }

    /** Builds the columns the first time the screen is shown, then loads the records. */
    @Override
    public void onShow() {
        if (!built) {
            buildColumns(table);
            addButton.setVisible(canAdd());
            addButton.setManaged(canAdd());
            built = true;
        }
        refresh();
    }
}
