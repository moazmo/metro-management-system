package metro.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import metro.service.Session;

/**
 * The menu on the left of the main window.
 *
 * It is built from a list of NavItems and shows only the items the logged-in
 * user is allowed to use. A section heading appears only when at least one
 * item under it is visible.
 */
public class Sidebar extends VBox {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final Map<NavItem, Button> buttons = new HashMap<>();

    public Sidebar(List<NavItem> items, Session session, Consumer<NavItem> onSelect) {
        getStyleClass().add("sidebar");
        String currentSection = null;
        for (NavItem item : items) {
            if (!item.isVisibleTo(session)) {
                continue;
            }
            String section = item.getSection();
            if (section != null && !section.equals(currentSection)) {
                Label heading = new Label(section);
                heading.getStyleClass().add("sidebar-section");
                getChildren().add(heading);
            }
            currentSection = section;

            Button button = new Button(item.getLabel(), Icons.of(item.getIcon(), 18));
            button.getStyleClass().add("nav-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> onSelect.accept(item));
            buttons.put(item, button);
            getChildren().add(button);
        }
    }

    /** Highlights the given item and removes the highlight from the others. */
    public void select(NavItem item) {
        for (Map.Entry<NavItem, Button> entry : buttons.entrySet()) {
            entry.getValue().pseudoClassStateChanged(SELECTED, entry.getKey() == item);
        }
    }
}
