package metro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import metro.MutableClock;
import metro.model.AuditEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditLogTest {

    @TempDir
    Path dir;
    MutableClock clock;
    AuditLog log;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(LocalDateTime.of(2026, 11, 15, 10, 0));
        log = new AuditLog(dir.resolve("audit.log"), clock);
    }

    @Test
    void appendsInOrder() {
        log.record("admin", "FIRST", "one");
        clock.plusMinutes(5);
        log.record("agent", "SECOND", "two");
        List<AuditEntry> entries = new AuditLog(dir.resolve("audit.log"), clock).readAll();
        assertEquals(2, entries.size());
        assertEquals("FIRST", entries.get(0).getAction());
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 0), entries.get(0).getTime());
        assertEquals("agent", entries.get(1).getUsername());
        assertEquals("two", entries.get(1).getDetail());
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 5), entries.get(1).getTime());
    }

    @Test
    void missingFileIsEmpty() {
        assertTrue(log.readAll().isEmpty());
    }

    @Test
    void detailWithSeparatorSurvives() {
        log.record("admin", "X", "a|b");
        assertEquals("a|b", log.readAll().get(0).getDetail());
    }
}
