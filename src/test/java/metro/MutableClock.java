package metro;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock for tests: it shows whatever time the test sets. */
public class MutableClock extends Clock {

    private final ZoneId zone = ZoneId.systemDefault();
    private LocalDateTime now;

    public MutableClock(LocalDateTime start) {
        this.now = start;
    }

    public void set(LocalDateTime time) {
        this.now = time;
    }

    public void plusMinutes(long minutes) {
        this.now = now.plusMinutes(minutes);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId other) {
        return this;
    }

    @Override
    public Instant instant() {
        return now.atZone(zone).toInstant();
    }
}
