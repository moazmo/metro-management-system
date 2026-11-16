package metro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {

    @TempDir
    Path dir;

    @Test
    void valueSurvivesReload() {
        new Settings(dir.resolve("settings.txt")).set("theme", "dark");
        assertEquals("dark", new Settings(dir.resolve("settings.txt")).get("theme", "light"));
    }

    @Test
    void missingFileGivesFallback() {
        assertEquals("light", new Settings(dir.resolve("settings.txt")).get("theme", "light"));
    }

    @Test
    void unknownLinesAreIgnored() throws IOException {
        Files.write(dir.resolve("settings.txt"), List.of("garbage", "theme=dark", "=x"), StandardCharsets.UTF_8);
        assertEquals("dark", new Settings(dir.resolve("settings.txt")).get("theme", "light"));
    }

    @Test
    void settingAgainReplacesTheValue() {
        Settings settings = new Settings(dir.resolve("settings.txt"));
        settings.set("theme", "dark");
        settings.set("theme", "light");
        settings.set("other", "1");
        Settings reloaded = new Settings(dir.resolve("settings.txt"));
        assertEquals("light", reloaded.get("theme", "dark"));
        assertEquals("1", reloaded.get("other", "0"));
    }
}
