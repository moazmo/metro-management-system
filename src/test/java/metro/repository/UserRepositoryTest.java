package metro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import metro.exception.DataFileException;
import metro.model.Role;
import metro.model.StationSupervisor;
import metro.model.User;
import metro.model.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserRepositoryTest {

    @TempDir
    Path dir;
    Path file;
    UserRepository repo;

    @BeforeEach
    void setUp() {
        file = dir.resolve("users.txt");
        repo = new UserRepository(file);
        repo.load();
    }

    private User add(Role role, String username, boolean active) {
        User user = UserFactory.create(role, repo.nextId(), username, "hash", "salt", "Full Name", "0100", "a@b.c", active);
        repo.save(user);
        return user;
    }

    private UserRepository reload() {
        UserRepository fresh = new UserRepository(file);
        fresh.load();
        return fresh;
    }

    @Test
    void roleSurvivesReload() {
        add(Role.STATION_SUPERVISOR, "supervisor", true);
        User loaded = reload().findAll().get(0);
        assertTrue(loaded instanceof StationSupervisor);
        assertEquals(Role.STATION_SUPERVISOR, loaded.getRole());
        assertEquals("Full Name", loaded.getFullName());
        assertEquals("0100", loaded.getPhone());
        assertEquals("a@b.c", loaded.getEmail());
    }

    @Test
    void inactiveFlagSurvivesReload() {
        add(Role.TICKET_AGENT, "agent", false);
        assertFalse(reload().findAll().get(0).isActive());
    }

    @Test
    void findByUsernameIgnoresCase() {
        User admin = add(Role.ADMIN, "admin", true);
        assertSame(admin, repo.findByUsername("ADMIN"));
        assertSame(admin, repo.findByUsername("  admin "));
    }

    @Test
    void unknownRoleNamesFileAndLine() throws IOException {
        Files.write(file, List.of(
                "#next=2",
                "# id|username|passwordHash|salt|role|fullName|phone|email|active",
                "USR-001|boss|hash|salt|BOSS|The Boss|||true"), StandardCharsets.UTF_8);
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("users.txt line 3"), e.getMessage());
    }
}
