package metro.model;

import java.time.LocalDateTime;

/** One line of the audit log: who did what, and when. */
public class AuditEntry {

    private final LocalDateTime time;
    private final String username;
    private final String action;
    private final String detail;

    public AuditEntry(LocalDateTime time, String username, String action, String detail) {
        this.time = time;
        this.username = username;
        this.action = action;
        this.detail = detail;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
