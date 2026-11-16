package metro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import metro.exception.DataFileException;
import metro.model.Role;
import metro.model.User;
import metro.service.DemoData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppContextTest {

    @TempDir
    Path temp;
    MutableClock clock = new MutableClock(LocalDateTime.of(2026, 11, 15, 10, 0));

    @Test
    void firstRunCreatesFolderAndDemoAccounts() {
        Path dataDir = temp.resolve("data");
        AppContext context = new AppContext(dataDir, clock);
        assertTrue(Files.exists(dataDir.resolve("users.txt")));

        User admin = context.getAuthService().login("admin", DemoData.DEMO_PASSWORD);
        assertNotNull(admin);
        List<User> users = context.getUserService().listAll();
        assertEquals(4, users.size());
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (User user : users) {
            roles.add(user.getRole());
            assertTrue(user.isActive());
        }
        assertEquals(EnumSet.allOf(Role.class), roles);
    }

    @Test
    void secondRunKeepsChanges() {
        Path dataDir = temp.resolve("data");
        AppContext first = new AppContext(dataDir, clock);
        first.getAuthService().login("admin", DemoData.DEMO_PASSWORD);
        for (User user : first.getUserService().listAll()) {
            if (user.getUsername().equals("agent")) {
                first.getUserService().setActive(user.getId(), false);
            }
        }

        AppContext second = new AppContext(dataDir, clock);
        second.getAuthService().login("admin", DemoData.DEMO_PASSWORD);
        assertEquals(4, second.getUserService().listAll().size());
        for (User user : second.getUserService().listAll()) {
            if (user.getUsername().equals("agent")) {
                assertFalse(user.isActive());
            }
        }
    }

    @Test
    void damagedFileStopsStartUp() throws IOException {
        Path dataDir = temp.resolve("data");
        Files.createDirectories(dataDir);
        Files.write(dataDir.resolve("users.txt"),
                List.of("#next=2", "# header", "USR-001|admin|hash"), StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> new AppContext(dataDir, clock));
        assertTrue(e.getMessage().contains("users.txt line 3"), e.getMessage());
    }
}
