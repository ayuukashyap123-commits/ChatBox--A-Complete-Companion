package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.*;
import com.chatbox.repository.KnowledgeRepository;
import com.chatbox.repository.MemoryRepository;
import com.chatbox.repository.SettingsRepository;
import com.chatbox.repository.WellnessRepository;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 Quality Assurance & Regression Test Suite.
 * Covers bug fixes and edge cases identified during the Phase 5 QA review:
 * 1. Goal status persistence across all Status enums (ACTIVE, COMPLETED, ARCHIVED, ON_HOLD).
 * 2. Task validation and transactional cascading on goal deletion.
 * 3. Document duplicate checking and edge-case text excerpt handling.
 * 4. Privacy boundary verification for wellness journals and memory approvals.
 * 5. Safe offline mode fallback without unhandled exceptions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5RegressionTest {

    private static CoachService coachService;
    private static KnowledgeRepository knowledgeRepo;
    private static DocumentService documentService;
    private static MemoryRepository memoryRepo;
    private static WellnessRepository wellnessRepo;
    private static AiService aiService;
    private static SettingsRepository settingsRepo;

    @BeforeAll
    static void initAll() {
        DatabaseManager.getInstance().initialize();
        coachService = new CoachService();
        knowledgeRepo = new KnowledgeRepository();
        documentService = new DocumentService();
        memoryRepo = new MemoryRepository();
        wellnessRepo = new WellnessRepository();
        aiService = new AiService();
        settingsRepo = new SettingsRepository();
    }

    @Test
    @Order(1)
    void testGoalStatusUpdatePersistence() {
        // Direct regression test for Goal Status bug resolved in CoachController
        Goal goal = coachService.createGoal("P5_Regression_Goal", "Testing status update persistence",
                Goal.Category.CAREER, Goal.Status.ACTIVE, LocalDate.now().plusMonths(1).toString());
        assertNotNull(goal);
        assertTrue(goal.getId() > 0);
        assertEquals(Goal.Status.ACTIVE, goal.getStatus());

        // Update status to COMPLETED
        goal.setStatus(Goal.Status.COMPLETED);
        coachService.updateGoal(goal);
        Goal retrieved = coachService.getGoalById(goal.getId());
        assertNotNull(retrieved);
        assertEquals(Goal.Status.COMPLETED, retrieved.getStatus());

        // Update status to PAUSED
        goal.setStatus(Goal.Status.PAUSED);
        coachService.updateGoal(goal);
        retrieved = coachService.getGoalById(goal.getId());
        assertEquals(Goal.Status.PAUSED, retrieved.getStatus());

        // Update status to ABANDONED
        goal.setStatus(Goal.Status.ABANDONED);
        coachService.updateGoal(goal);
        retrieved = coachService.getGoalById(goal.getId());
        assertEquals(Goal.Status.ABANDONED, retrieved.getStatus());

        // Cleanup
        coachService.deleteGoal(goal.getId());
        assertNull(coachService.getGoalById(goal.getId()));
    }

    @Test
    @Order(2)
    void testTaskValidationAndCascadeDeletion() {
        Goal goal = coachService.createGoal("P5_Task_Goal", "Goal for testing tasks",
                Goal.Category.PERSONAL, Goal.Status.ACTIVE, LocalDate.now().plusWeeks(1).toString());

        // Invalid task titles
        assertThrows(IllegalArgumentException.class, () ->
            coachService.createTask(goal.getId(), "", "desc", Task.Priority.MEDIUM, Task.Status.TODO, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            coachService.createTask(goal.getId(), "   ", "desc", Task.Priority.MEDIUM, Task.Status.TODO, null)
        );

        // Valid task creation
        Task task = coachService.createTask(goal.getId(), "P5 Task 1", "Detailed step",
                Task.Priority.URGENT, Task.Status.TODO, LocalDate.now().toString());
        assertNotNull(task);
        assertTrue(task.getId() > 0);
        assertEquals(Task.Priority.URGENT, task.getPriority());

        // Update task status
        task.setStatus(Task.Status.IN_PROGRESS);
        coachService.updateTask(task);
        Task updatedTask = coachService.getTasksForGoal(goal.getId()).stream()
                .filter(t -> t.getId() == task.getId())
                .findFirst().orElse(null);
        assertNotNull(updatedTask);
        assertEquals(Task.Status.IN_PROGRESS, updatedTask.getStatus());

        // Cascade deletion when goal is deleted
        coachService.deleteGoal(goal.getId());
        List<Task> remainingTasks = coachService.getTasksForGoal(goal.getId());
        assertTrue(remainingTasks.isEmpty(), "Tasks must be deleted when the parent goal is deleted");
    }

    @Test
    @Order(3)
    void testDocumentDuplicateDetectionAndExcerptEdgeCases() {
        String testTitle = "P5_Test_Doc_Dup";
        String testPath = "C:/test/p5_dup.txt";

        // Pre-check
        assertFalse(knowledgeRepo.existsByTitleOrPath(testTitle, testPath));

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(testTitle);
        doc.setFilePath(testPath);
        doc.setFileType(KnowledgeDocument.FileType.TXT);
        doc.setContent("Content for duplicate detection testing in Phase 5 QA.");
        doc.setWordCount(9);
        knowledgeRepo.save(doc);

        try {
            // Document should now exist by title or path
            assertTrue(knowledgeRepo.existsByTitleOrPath(testTitle, "C:/different/path.txt"));
            assertTrue(knowledgeRepo.existsByTitleOrPath("Different Title", testPath));

            // Edge cases in extractRelevantExcerpt
            assertEquals("", documentService.extractRelevantExcerpt(null, "query"));
            KnowledgeDocument emptyDoc = new KnowledgeDocument();
            emptyDoc.setContent("");
            assertEquals("", documentService.extractRelevantExcerpt(emptyDoc, "query"));
            assertEquals("", documentService.extractRelevantExcerpt(doc, null));
            assertEquals("", documentService.extractRelevantExcerpt(doc, ""));
        } finally {
            knowledgeRepo.delete(doc.getId());
        }
    }

    @Test
    @Order(4)
    void testPrivacyIsolationAndMemoryApprovalEnforcement() {
        // Create an unapproved memory
        Memory unapproved = new Memory();
        unapproved.setContent("P5_Unapproved_Memory_Content_Should_Never_Leak");
        unapproved.setCategory(Memory.Category.FACT);
        unapproved.setApproved(false);
        memoryRepo.save(unapproved);

        // Create an approved memory
        Memory approved = new Memory();
        approved.setContent("P5_Approved_Memory_Content_For_Personalization");
        approved.setCategory(Memory.Category.PREFERENCE);
        approved.setApproved(true);
        memoryRepo.save(approved);

        // Create a sensitive journal entry
        JournalEntry journal = new JournalEntry();
        journal.setTitle("P5_Private_Journal_Entry");
        journal.setContent("Highly confidential personal reflections that must never enter AI prompts.");
        journal.setMoodScore(4);
        wellnessRepo.saveJournal(journal);

        try {
            UserProfile profile = new UserProfile();
            profile.setName("Ayush");
            profile.setPreferredName("Ayush");

            String prompt = aiService.buildSystemPrompt(Conversation.Mode.GENERAL, profile);

            // 1. Approved memory IS in the prompt
            assertTrue(prompt.contains("P5_Approved_Memory_Content_For_Personalization"),
                    "Approved memory must be included in system prompt");

            // 2. Unapproved memory is NOT in the prompt
            assertFalse(prompt.contains("P5_Unapproved_Memory_Content_Should_Never_Leak"),
                    "Unapproved memory must strictly be excluded from system prompt");

            // 3. Private journal is NOT in the prompt
            assertFalse(prompt.contains("P5_Private_Journal_Entry"),
                    "Journal title must never be included in system prompt");
            assertFalse(prompt.contains("Highly confidential personal reflections"),
                    "Journal body must never be included in system prompt");

            // 4. Memory approval toggle test
            unapproved.setApproved(true);
            memoryRepo.update(unapproved);
            String promptAfterApproval = aiService.buildSystemPrompt(Conversation.Mode.GENERAL, profile);
            assertTrue(promptAfterApproval.contains("P5_Unapproved_Memory_Content_Should_Never_Leak"),
                    "Once approved, memory should now be included");

        } finally {
            memoryRepo.delete(unapproved.getId());
            memoryRepo.delete(approved.getId());
            wellnessRepo.deleteJournal(journal.getId());
        }
    }

    @Test
    @Order(5)
    void testOfflineFallbackResilience() {
        // Verify that offline demo mode responds reliably without crashing
        UserProfile profile = new UserProfile();
        profile.setName("Ayush");

        for (Conversation.Mode mode : Conversation.Mode.values()) {
            String response = aiService.buildDemoResponse(mode, "Hello companion", profile);
            assertNotNull(response, "Response for mode " + mode + " must not be null");
            assertTrue(response.contains(AiService.DEMO_LABEL),
                    "Offline demo response must include the standard offline label for mode " + mode);
        }

        // Help command
        String helpResponse = aiService.buildDemoResponse(Conversation.Mode.GENERAL, "/help", profile);
        assertNotNull(helpResponse);
        assertTrue(helpResponse.contains("offline commands"));

        // Goals command
        String goalsResponse = aiService.buildDemoResponse(Conversation.Mode.GENERAL, "/goals", profile);
        assertNotNull(goalsResponse);
        assertTrue(goalsResponse.contains("Actionable Goal Decomposition"));
    }
}
