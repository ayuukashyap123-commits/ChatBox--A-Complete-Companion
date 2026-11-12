package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoryTest {

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
    }

    @Test
    @Order(1)
    void testUserProfileRepository() {
        UserProfileRepository repo = new UserProfileRepository();
        UserProfile profile = repo.getProfile();
        assertNotNull(profile);
        assertNotNull(profile.getDisplayName());
        assertFalse(profile.getDisplayName().isBlank());

        // Update profile
        String oldName = profile.getName();
        profile.setName("Test Student");
        repo.saveProfile(profile);

        UserProfile reloaded = repo.getProfile();
        assertEquals("Test Student", reloaded.getName());

        // Restore
        profile.setName(oldName);
        repo.saveProfile(profile);
    }

    @Test
    @Order(2)
    void testCoachRepositoryGoalsAndTasks() {
        CoachRepository repo = new CoachRepository();

        Goal goal = new Goal();
        goal.setTitle("Unit Test Goal");
        goal.setDescription("Goal created by automated test");
        goal.setCategory(Goal.Category.PERSONAL);
        goal.setStatus(Goal.Status.ACTIVE);
        goal.setTargetDate(LocalDate.now().plusDays(30).toString());
        goal.setProgress(25);

        Goal saved = repo.saveGoal(goal);
        assertTrue(saved.getId() > 0, "Saved goal should have positive generated ID");

        Task task = new Task();
        task.setGoalId(saved.getId());
        task.setTitle("Unit Test Task");
        task.setDescription("Task for the unit test goal");
        task.setPriority(Task.Priority.HIGH);
        task.setStatus(Task.Status.TODO);
        task.setDueDate(LocalDate.now().plusDays(7).toString());

        Task savedTask = repo.saveTask(task);
        assertTrue(savedTask.getId() > 0, "Saved task should have positive generated ID");

        List<Task> tasks = repo.findAllTasks();
        boolean foundTask = tasks.stream().anyMatch(t -> t.getId() == savedTask.getId());
        assertTrue(foundTask, "Saved task should be present in list");

        // Clean up
        repo.deleteTask(savedTask.getId());
        repo.deleteGoal(saved.getId());
    }

    @Test
    @Order(3)
    void testCoachRepositoryHabits() {
        CoachRepository repo = new CoachRepository();

        Habit habit = new Habit();
        habit.setTitle("Test Habit - Drink 2L Water");
        habit.setDescription("Stay hydrated");
        habit.setFrequency(Habit.Frequency.DAILY);

        Habit saved = repo.saveHabit(habit);
        assertTrue(saved.getId() > 0, "Saved habit should have positive generated ID");

        // Toggle complete
        repo.toggleHabitCompletion(saved.getId(), true);
        List<Habit> habits = repo.findAllHabits();
        Habit retrieved = habits.stream().filter(h -> h.getId() == saved.getId()).findFirst().orElse(null);
        assertNotNull(retrieved);
        assertTrue(retrieved.isCompletedToday(), "Habit should be marked completed today");

        // Toggle incomplete
        repo.toggleHabitCompletion(saved.getId(), false);
        habits = repo.findAllHabits();
        retrieved = habits.stream().filter(h -> h.getId() == saved.getId()).findFirst().orElse(null);
        assertNotNull(retrieved);
        assertFalse(retrieved.isCompletedToday(), "Habit should be unmarked");

        // Clean up
        repo.deleteHabit(saved.getId());
    }

    @Test
    @Order(4)
    void testWellnessRepositoryMoodAndJournal() {
        WellnessRepository repo = new WellnessRepository();

        MoodEntry mood = new MoodEntry();
        mood.setMoodScore(4);
        mood.setMoodLabel("Good");
        mood.setNote("Automated test mood note");
        mood.setTags("test, junit");

        MoodEntry savedMood = repo.saveMood(mood);
        assertTrue(savedMood.getId() > 0);

        List<MoodEntry> moods = repo.findRecentMoods(10);
        assertFalse(moods.isEmpty(), "Recent moods should not be empty");

        JournalEntry journal = new JournalEntry();
        journal.setTitle("Test Journal Entry");
        journal.setContent("Testing journal persistence in SQLite database.");
        journal.setMoodScore(4);
        journal.setTags("test");

        JournalEntry savedJournal = repo.saveJournal(journal);
        assertTrue(savedJournal.getId() > 0);

        List<JournalEntry> journals = repo.findAllJournals();
        boolean foundJournal = journals.stream().anyMatch(j -> j.getId() == savedJournal.getId());
        assertTrue(foundJournal, "Saved journal should exist");

        // Clean up
        repo.deleteJournal(savedJournal.getId());
    }

    @Test
    @Order(5)
    void testMemoryRepository() {
        MemoryRepository repo = new MemoryRepository();

        Memory memory = new Memory();
        memory.setContent("User prefers dark mode interfaces.");
        memory.setCategory(Memory.Category.PREFERENCE);
        memory.setSource("MANUAL");
        memory.setApproved(true);

        Memory saved = repo.save(memory);
        assertTrue(saved.getId() > 0);

        List<Memory> approved = repo.findApproved();
        assertTrue(approved.stream().anyMatch(m -> m.getId() == saved.getId()));

        // Toggle approved off
        saved.setApproved(false);
        repo.update(saved);

        approved = repo.findApproved();
        assertFalse(approved.stream().anyMatch(m -> m.getId() == saved.getId()), "Unapproved memory should not be returned by findApproved()");

        // Clean up
        repo.delete(saved.getId());
    }

    @Test
    @Order(6)
    void testConversationRepository() {
        ConversationRepository repo = new ConversationRepository();

        Conversation conv = new Conversation();
        conv.setTitle("Test Conversation");
        conv.setMode(Conversation.Mode.GENERAL);

        Conversation saved = repo.save(conv);
        assertTrue(saved.getId() > 0);

        Message msg = new Message();
        msg.setConversationId(saved.getId());
        msg.setRole(Message.Role.USER);
        msg.setContent("Hello, this is a test prompt.");

        Message savedMsg = repo.saveMessage(msg);
        assertTrue(savedMsg.getId() > 0);

        List<Message> messages = repo.findMessages(saved.getId());
        assertEquals(1, messages.size());
        assertEquals("Hello, this is a test prompt.", messages.get(0).getContent());

        // Clean up
        repo.delete(saved.getId());
        assertEquals(0, repo.findMessages(saved.getId()).size(), "Messages should cascade-delete with conversation");
    }
}
