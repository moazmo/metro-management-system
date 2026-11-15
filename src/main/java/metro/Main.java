package metro;

import javafx.application.Application;
import metro.ui.MetroApp;

/**
 * Starting point of the program.
 *
 * This class is deliberately not a JavaFX Application itself: a plain class
 * with a main method can be packaged and double-clicked later, while the
 * real window is created by {@link MetroApp}.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(MetroApp.class, args);
    }
}
