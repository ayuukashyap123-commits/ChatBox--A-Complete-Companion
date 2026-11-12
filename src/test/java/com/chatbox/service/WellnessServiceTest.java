package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.JournalEntry;
import com.chatbox.model.MoodEntry;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WellnessServiceTest {

    private static WellnessService service;

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
        service = new WellnessService();
    }

    @Test
    @Order(1)
    void testMoodLoggingAndCalculation() {
        MoodEntry m1 = service.logMood(4, "Productive check-in");
        MoodEntry m2 = service.logMood(5, "Energized check-in");

        assertNotNull(m1);
        assertTrue(m1.getId() > 0);
        assertEquals(4, m1.getMoodScore());

        assertNotNull(m2);
        assertTrue(m2.getId() > 0);
        assertEquals(5, m2.getMoodScore());

        WellnessService.MoodSummary summary = service.calculateMoodSummary();
        assertNotNull(summary);
        assertTrue(summary.totalCheckIns() >= 2);
        assertTrue(summary.averageScore() >= 1.0 && summary.averageScore() <= 10.0);
        assertNotNull(summary.dominantMood());
        assertNotNull(summary.latestEntry());
    }

    @Test
    @Order(2)
    void testJournalCRUDAndPrivacySovereignty() {
        assertThrows(IllegalArgumentException.class, () -> service.saveJournal("Title", "", 5, null));
        assertThrows(IllegalArgumentException.class, () -> service.saveJournal("Title", "   ", 5, null));

        JournalEntry journal = service.saveJournal("Testing Reflections", "Private thoughts that must never leak.", 4, "privacy, reflection");
        assertNotNull(journal);
        assertTrue(journal.getId() > 0);

        JournalEntry retrieved = service.getJournalById(journal.getId());
        assertNotNull(retrieved);
        assertEquals("Testing Reflections", retrieved.getTitle());
        assertEquals("Private thoughts that must never leak.", retrieved.getContent());

        // Update
        retrieved.setTitle("Updated Title");
        service.updateJournal(retrieved);
        assertEquals("Updated Title", service.getJournalById(journal.getId()).getTitle());

        // Delete
        service.deleteJournal(journal.getId());
        assertNull(service.getJournalById(journal.getId()));
    }
}
