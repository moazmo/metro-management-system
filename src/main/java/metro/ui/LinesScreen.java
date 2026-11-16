package metro.ui;

import java.util.List;
import javafx.scene.control.TableView;
import metro.AppContext;
import metro.model.Line;
import metro.service.LineService;

/**
 * Specification 1b: admins create and manage lines and their station sequences.
 */
public class LinesScreen extends CrudScreen<Line> {

    private final LineService service;

    public LinesScreen(AppContext context) {
        super(context, "Lines", "Add line");
        this.service = context.getLineService();
        setEmptyText("No lines yet.");
    }

    @Override
    protected List<Line> loadItems(String searchText) {
        return service.search(searchText);
    }

    @Override
    protected void buildColumns(TableView<Line> table) {
        addColumn(table, "ID", line -> line.getId());
        addNodeColumn(table, "Name", line -> new LineSwatch(line));
        addColumn(table, "Stations", line -> String.valueOf(line.size()));
        addColumn(table, "Route", line -> line.getFirstStation().getName() + " → " + line.getLastStation().getName());
    }

    @Override
    protected void openForm(Line existingOrNull) {
        LineFormDialog dialog = new LineFormDialog(service, context.getStationService().listAll(), existingOrNull);
        if (showDialog(dialog)) {
            refresh();
            showInfo(existingOrNull == null ? "Line added." : "Line updated.");
        }
    }

    @Override
    protected void deleteItem(Line line) {
        service.delete(line.getId());
    }

    @Override
    protected String deleteQuestion(Line line) {
        return "Delete " + line.getName() + "?";
    }
}
