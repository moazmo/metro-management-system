package metro.ui;

import java.util.function.Supplier;
import metro.model.Permission;
import metro.service.Session;

/**
 * One entry of the sidebar: its section, label and icon, the permission a
 * user needs to see it, and how to create the screen it opens.
 */
public class NavItem {

    private final String section;
    private final String label;
    private final String icon;
    private final Permission permission;
    private final Supplier<Screen> screen;

    /**
     * @param section    heading the item appears under, or null for none
     * @param permission permission needed to see the item, or null when every logged-in user may
     * @param screen     creates the screen when the item is clicked
     */
    public NavItem(String section, String label, String icon, Permission permission, Supplier<Screen> screen) {
        this.section = section;
        this.label = label;
        this.icon = icon;
        this.permission = permission;
        this.screen = screen;
    }

    public String getSection() {
        return section;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    /** Rule: an item is shown only to users whose role has its permission. */
    public boolean isVisibleTo(Session session) {
        return permission == null || session.can(permission);
    }

    public Screen createScreen() {
        return screen.get();
    }
}
