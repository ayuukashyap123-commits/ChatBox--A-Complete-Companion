package com.chatbox.database;

import com.chatbox.model.*;
import com.chatbox.repository.*;
import com.chatbox.service.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end persistence verification across application restarts.
 * Verifies that all 7 core modules persist data cleanly to SQLite
 * and reload correctly after database disconnection and reconnection.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistenceRestartTest {

    private static int testConvId;
    private static int testGoalId;
    private static int testTaskId;
    private static int testHabitId;
    private static int testMoodId;
    private static int testJournalId;
    private static int testMemoryId;
    private static int testDocId;

    @Test
    @Order(1)
    void step1_SaveAllModuleDataBeforeRestart() {
        DatabaseManager.getInstance().initialize();

        // 1. AI Chatbox
        ConversationRepository convRepo = new ConversationRepository();
        Conversation conv = new Conversation("Restart Test Conversation", Conversation.Mode.COACH);
        convRepo.save(conv);
        testConvId = conv.getId();
        assertTrue(testConvId > 0);

        Message m1 = new Message(testConvId, Message.Role.USER, "How do I maintain persistence?");
        convRepo.saveMessage(m1);
        Message m2 = new Message(testConvId, Message.Role.ASSISTANT, "Write transactional SQLite services.");
        convRepo.saveMessage(m2);

        // 2. Digital Doppelgänger
        ProfileService profileService = new ProfileService();
        profileService.saveProfile("Ayush Persistence Test", "Ayush", 22, "Systems Engineer",
                "Senior capstone bio", "Distributed Systems, AI, JavaFX", "Analytical", "Concise", "Deliver 100% working MVP");

        // 3. Mental Wellness
        WellnessService wellnessService = new WellnessService();
        MoodEntry mood = wellnessService.logMood(5, "Energized & Confident", "Pre-restart test note", "restart, junit");
        testMoodId = mood.getId();
        assertTrue(testMoodId > 0);

        JournalEntry journal = wellnessService.saveJournal("Pre-Restart Reflection", "Validating that data survives shutdown.", 5, "persistence");
        testJournalId = journal.getId();
        assertTrue(testJournalId > 0);

        // 4. Life Coach
        CoachService coachService = new CoachService();
        Goal goal = coachService.createGoal("Deliver Production Companion", "Full persistence verification",
                Goal.Category.LEARNING, Goal.Status.ACTIVE, LocalDate.now().plusWeeks(2).toString());
        testGoalId = goal.getId();
        assertTrue(testGoalId > 0);

        Task task = coachService.createTask(testGoalId, "Verify Restart Persistence", "Simulate JVM shutdown and reboot",
                Task.Priority.HIGH, Task.Status.TODO, LocalDate.now().plusDays(1).toString());
        testTaskId = task.getId();
        assertTrue(testTaskId > 0);

        // Complete the task and ensure progress recalculates
        coachService.setTaskCompletion(testTaskId, true);
        assertEquals(100, coachService.getGoalById(testGoalId).getProgress());

        Habit habit = coachService.createHabit("Daily Verification Run", "Execute full test suites", Habit.Frequency.DAILY);
        testHabitId = habit.getId();
        assertTrue(testHabitId > 0);
        coachService.toggleHabitCompletion(testHabitId, true);

        // 5. Second Brain
        KnowledgeRepository knowledgeRepo = new KnowledgeRepository();
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle("Architecture Decisions.md");
        doc.setFilePath("sample-documents/ai-desktop-systems.md");
        doc.setFileType(KnowledgeDocument.FileType.MD);
        doc.setContent("JavaFX native desktop applications with SQLite WAL mode persistence provide optimal local-first latency.");
        doc.setWordCount(14);
        knowledgeRepo.save(doc);
        testDocId = doc.getId();
        assertTrue(testDocId > 0);

        // 6. Memory Management
        MemoryService memoryService = new MemoryService();
        Memory memory = memoryService.createMemory("User values transparent offline-first data sovereignty.", Memory.Category.PREFERENCE, true);
        testMemoryId = memory.getId();
        assertTrue(testMemoryId > 0);

        // Simulate complete application shutdown
        DatabaseManager.getInstance().close();
    }

    @Test
    @Order(2)
    void step2_VerifyAllModuleDataReloadsAfterRestart() {
        // Re-open fresh database connection (simulates app reboot)
        DatabaseManager.getInstance().initialize();

        // 1. Verify AI Chatbox
        ConversationRepository convRepo = new ConversationRepository();
        Conversation conv = convRepo.findById(testConvId);
        assertNotNull(conv, "Conversation must survive application restart");
        assertEquals("Restart Test Conversation", conv.getTitle());
        assertEquals(Conversation.Mode.COACH, conv.getMode());

        List<Message> messages = convRepo.findMessages(testConvId);
        assertEquals(2, messages.size(), "All messages must survive restart");
        assertEquals("How do I maintain persistence?", messages.get(0).getContent());
        assertEquals("Write transactional SQLite services.", messages.get(1).getContent());

        // 2. Verify Digital Doppelgänger
        ProfileService profileService = new ProfileService();
        UserProfile profile = profileService.getProfile();
        assertNotNull(profile);
        assertEquals("Ayush Persistence Test", profile.getName());
        assertEquals("Systems Engineer", profile.getOccupation());

        // 3. Verify Mental Wellness
        WellnessService wellnessService = new WellnessService();
        MoodEntry mood = wellnessService.getRecentMoods(50).stream()
                .filter(m -> m.getId() == testMoodId)
                .findFirst()
                .orElse(null);
        assertNotNull(mood, "Mood check-in must survive restart");
        assertEquals(5, mood.getMoodScore());
        assertEquals("Pre-restart test note", mood.getNote());

        JournalEntry journal = wellnessService.getJournalById(testJournalId);
        assertNotNull(journal, "Journal entry must survive restart");
        assertEquals("Pre-Restart Reflection", journal.getTitle());

        // 4. Verify Life Coach
        CoachService coachService = new CoachService();
        Goal goal = coachService.getGoalById(testGoalId);
        assertNotNull(goal, "Goal must survive restart");
        assertEquals(100, goal.getProgress(), "Goal progress percentage must survive restart");

        Task task = coachService.getTaskById(testTaskId);
        assertNotNull(task, "Task must survive restart");
        assertEquals(Task.Status.DONE, task.getStatus());

        List<Habit> habits = coachService.getAllHabits();
        Habit habit = habits.stream().filter(h -> h.getId() == testHabitId).findFirst().orElse(null);
        assertNotNull(habit, "Habit must survive restart");
        assertTrue(habit.isCompletedToday(), "Habit today's completion must survive restart");

        // 5. Verify Second Brain
        KnowledgeRepository knowledgeRepo = new KnowledgeRepository();
        KnowledgeDocument doc = knowledgeRepo.findById(testDocId);
        assertNotNull(doc, "Knowledge document must survive restart");
        List<KnowledgeDocument> searchResults = knowledgeRepo.search("WAL mode");
        assertFalse(searchResults.isEmpty(), "Document content must remain searchable after restart");

        // 6. Verify Memory Management
        MemoryService memoryService = new MemoryService();
        Memory memory = memoryService.getMemoryById(testMemoryId);
        assertNotNull(memory, "Memory must survive restart");
        assertTrue(memory.isApproved(), "Approval state must survive restart");

        // Clean up test records
        convRepo.delete(testConvId);
        coachService.deleteGoal(testGoalId);
        coachService.deleteHabit(testHabitId);
        wellnessService.deleteJournal(testJournalId);
        knowledgeRepo.delete(testDocId);
        memoryService.deleteMemory(testMemoryId);
    }
}
