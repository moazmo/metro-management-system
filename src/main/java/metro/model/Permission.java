package metro.model;

/**
 * One thing a user may be allowed to do. Every role owns a set of these.
 * Menus are shown, and service methods are allowed, according to them.
 */
public enum Permission {
    MANAGE_STATIONS,
    MANAGE_LINES,
    MANAGE_TRAINS,
    MANAGE_USERS,
    MANAGE_FARES,
    VIEW_AUDIT_LOG,
    MANAGE_TRIPS,
    VIEW_TRIPS,
    VIEW_NETWORK,
    SELL_TICKETS,
    VALIDATE_TICKETS,
    REFUND_TICKETS,
    VIEW_REPORTS,
    EDIT_OWN_PROFILE
}
