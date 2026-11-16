package metro.ui;

import javafx.scene.Node;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Gives the screens their icons.
 *
 * This is the only class that knows about the third-party icon library.
 * Screens ask for an icon by a simple name such as "train"; if the library
 * had to be removed, only this class would change.
 */
public final class Icons {

    private Icons() {
    }

    /**
     * An icon by its Material Design name, for example "dashboard" or "train".
     * An unknown name gives a plain dot, never an error.
     */
    public static Node of(String name, int size) {
        try {
            FontIcon icon = new FontIcon(codeFor(name));
            icon.setIconSize(size);
            // The theme sets its own icon size, so app.css has one class per size we use.
            icon.getStyleClass().addAll("icon", "icon-" + size);
            return icon;
        } catch (RuntimeException e) {
            Circle dot = new Circle(size / 4.0);
            dot.getStyleClass().add("icon-dot");
            return dot;
        }
    }

    /** The icon library keeps names starting a-l and m-z in two separate sets. */
    private static String codeFor(String name) {
        char first = name.charAt(0);
        boolean firstHalf = Character.isDigit(first) || (first >= 'a' && first <= 'l');
        return (firstHalf ? "mdal-" : "mdmz-") + name;
    }
}
