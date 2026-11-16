package metro.ui;

import javafx.scene.control.Label;

/**
 * A small coloured label used to show a status such as "Active" or "Delayed".
 */
public class StatusBadge extends Label {

    /** The colour family of a badge. */
    public enum Tone { NEUTRAL, GOOD, WARNING, BAD }

    public StatusBadge(String text, Tone tone) {
        super(text);
        getStyleClass().addAll("badge", "badge-" + tone.name().toLowerCase());
    }
}
