package metro.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;

/**
 * Controls the look of the whole application (light or dark).
 *
 * This is the only class that knows about the third-party theme library.
 * If the library had to be removed, only this class would change.
 */
public final class Theme {

    private static final String APP_CSS = "/metro/ui/app.css";
    private static boolean dark = false;

    private Theme() {
    }

    /** Applies the light or dark theme to a scene and adds our own style sheet. */
    public static void apply(Scene scene, boolean useDark) {
        dark = useDark;
        if (dark) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
        String css = appStyleSheet();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
        scene.getRoot().getStyleClass().remove("dark");
        if (dark) {
            scene.getRoot().getStyleClass().add("dark");
        }
    }

    /** The address of our own style sheet, for windows (dialogs) that are not the main scene. */
    public static String appStyleSheet() {
        return Theme.class.getResource(APP_CSS).toExternalForm();
    }

    public static boolean isDark() {
        return dark;
    }

    /** Switches between light and dark. */
    public static void toggle(Scene scene) {
        apply(scene, !dark);
    }
}
