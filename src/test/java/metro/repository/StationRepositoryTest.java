package metro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import metro.exception.DataFileException;
import metro.exception.NotFoundException;
import metro.model.Station;
import metro.model.StationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StationRepositoryTest {

    @TempDir
    Path dir;
    Path file;
    StationRepository repo;

    @BeforeEach
    void setUp() {
        file = dir.resolve("stations.txt");
        repo = new StationRepository(file);
        repo.load();
    }

    private Station add(String name, String nameAr) {
        Station station = new Station(repo.nextId(), name, nameAr, StationStatus.OPEN);
        repo.save(station);
        return station;
    }

    private StationRepository reload() {
        StationRepository fresh = new StationRepository(file);
        fresh.load();
        return fresh;
    }

    private void writeFile(String... lines) throws IOException {
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
    }

    @Test
    void savedStationsSurviveReload() {
        add("Helwan", "حلوان");
        add("Sadat", "السادات");
        List<Station> loaded = reload().findAll();
        assertEquals(2, loaded.size());
        assertEquals("Helwan", loaded.get(0).getName());
        assertEquals("حلوان", loaded.get(0).getNameAr());
        assertEquals("Sadat", loaded.get(1).getName());
        assertEquals("السادات", loaded.get(1).getNameAr());
    }

    @Test
    void fileIsReadableText() throws IOException {
        add("Helwan", "حلوان");
        assertEquals(
                List.of("#next=2", "# id|name|nameAr|status", "STN-001|Helwan|حلوان|OPEN"),
                Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    void updateKeepsPosition() throws IOException {
        Station a = add("A", "");
        add("B", "");
        a.setName("A2");
        repo.save(a);
        assertEquals("A2", repo.findAll().get(0).getName());
        assertEquals("B", repo.findAll().get(1).getName());
        assertTrue(Files.readAllLines(file, StandardCharsets.UTF_8).get(2).startsWith("STN-001|A2|"));
    }

    @Test
    void deleteRemovesTheLine() throws IOException {
        add("A", "");
        add("B", "");
        repo.delete("STN-001");
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            assertTrue(!line.startsWith("STN-001"), "line still present: " + line);
        }
        assertNull(repo.findById("STN-001"));
    }

    @Test
    void deleteUnknownIsNotFound() {
        assertThrows(NotFoundException.class, () -> repo.delete("STN-999"));
    }

    @Test
    void requireUnknownIsNotFound() {
        NotFoundException e = assertThrows(NotFoundException.class, () -> repo.require("STN-999"));
        assertEquals("Station STN-999 was not found", e.getMessage());
    }

    @Test
    void idsAreNeverReused() {
        add("A", "");
        add("B", "");
        repo.delete("STN-002");
        assertEquals("STN-003", reload().nextId());
    }

    @Test
    void missingFileMeansEmpty() {
        assertTrue(repo.findAll().isEmpty());
        assertEquals("STN-001", repo.nextId());
    }

    @Test
    void separatorInNameSurvives() {
        add("A|B", "");
        assertEquals("A|B", reload().findAll().get(0).getName());
    }

    @Test
    void findByNameIgnoresCaseAndSpaces() {
        Station helwan = add("Helwan", "حلوان");
        assertSame(helwan, repo.findByName("  helwan "));
        assertNull(repo.findByName("Nowhere"));
    }

    @Test
    void damagedLineNamesFileAndLine() throws IOException {
        writeFile("#next=2", "# id|name|nameAr|status", "STN-001|Helwan");
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("stations.txt line 3"), e.getMessage());
        assertTrue(e.getMessage().contains("expected 4 fields but found 2"), e.getMessage());
    }

    @Test
    void fileSavedByAnEditorWithByteOrderMarkStillLoads() throws IOException {
        writeFile("﻿#next=2", "# id|name|nameAr|status", "STN-001|Helwan|حلوان|OPEN");
        StationRepository loaded = reload();
        assertEquals(1, loaded.findAll().size());
        assertEquals("STN-002", loaded.nextId());
    }

    @Test
    void missingCounterLineStillGivesFreshIds() throws IOException {
        writeFile("STN-007|Helwan|حلوان|OPEN");
        assertEquals("STN-008", reload().nextId());
    }

    @Test
    void unknownStatusNamesFileAndLine() throws IOException {
        writeFile("#next=2", "# id|name|nameAr|status", "STN-001|Helwan||OPENED");
        DataFileException e = assertThrows(DataFileException.class, () -> reload());
        assertTrue(e.getMessage().contains("stations.txt line 3"), e.getMessage());
    }
}
