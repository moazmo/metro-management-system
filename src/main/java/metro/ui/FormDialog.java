package metro.ui;

import java.util.List;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import metro.exception.MetroException;

/**
 * Base class of the pop-up forms used to add or edit a record.
 *
 * A subclass adds its fields with addText, addChoice and so on, and writes
 * a save() method that calls a service. If the service refuses (it throws a
 * MetroException), the message is shown inside the dialog and the dialog
 * stays open so the user can correct the input.
 */
public abstract class FormDialog extends Dialog<Boolean> {

    private final GridPane grid = new GridPane();
    private final Label error = new Label();
    private int nextRow = 0;
    private boolean saved = false;

    protected FormDialog(String title) {
        setTitle(title);
        setHeaderText(null);

        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(110);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setMinWidth(300);
        grid.getColumnConstraints().addAll(labels, fields);

        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.setMaxWidth(430);
        error.setVisible(false);
        error.setManaged(false);

        VBox body = new VBox(14, grid, error);
        body.setPadding(new Insets(20, 24, 8, 24));
        getDialogPane().setContent(body);
        getDialogPane().getStylesheets().add(Theme.appStyleSheet());

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        Button saveButton = (Button) getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("accent");
        // Runs before the dialog closes: a refused save keeps the dialog open.
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                save();
                saved = true;
            } catch (MetroException e) {
                showError(e.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> saved);
    }

    /** Stores what the user entered by calling a service. Throw a MetroException to refuse. */
    protected abstract void save();

    protected final void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    protected final TextField addText(String label, String initialValue) {
        TextField field = new TextField(initialValue == null ? "" : initialValue);
        addRow(label, field);
        return field;
    }

    protected final PasswordField addPassword(String label) {
        PasswordField field = new PasswordField();
        addRow(label, field);
        return field;
    }

    protected final <E> ComboBox<E> addChoice(String label, List<E> options, E initialValue) {
        ComboBox<E> box = new ComboBox<>();
        box.getItems().addAll(options);
        box.setValue(initialValue);
        box.setMaxWidth(Double.MAX_VALUE);
        addRow(label, box);
        return box;
    }

    protected final Spinner<Integer> addNumber(String label, int min, int max, int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        // Typed text is taken over when the field loses focus, not only on Enter.
        spinner.focusedProperty().addListener((observable, was, is) -> {
            if (!is) {
                spinner.increment(0);
            }
        });
        addRow(label, spinner);
        return spinner;
    }

    /** Adds any control with a label in front of it. */
    protected final void addRow(String label, Node control) {
        Label text = new Label(label);
        grid.add(text, 0, nextRow);
        grid.add(control, 1, nextRow);
        nextRow++;
    }

    /** Shows the dialog and waits. True when the record was saved. */
    public boolean showAndSave() {
        showAndWait();
        return saved;
    }
}
