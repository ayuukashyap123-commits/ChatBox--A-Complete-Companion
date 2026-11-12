package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Goal;
import com.chatbox.model.Habit;
import com.chatbox.model.Task;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoachServiceTest {

    private static CoachService service;

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
        service = new CoachService();
    }

    @Test
    @Order(1)
    void testGoalCreationAndValidation() {
        assertThrows(IllegalArgumentException.class, () -> service.createGoal("", "desc", Goal.Category.CAREER, Goal.Status.ACTIVE, "2026-12-31"));
        assertThrows(IllegalArgumentException.class, () -> service.createGoal("   ", "desc", Goal.Category.CAREER, Goal.Status.ACTIVE, "2026-12-31"));

        Goal goal = service.createGoal("Master Distributed Systems", "Complete 5 distributed systems projects",
                Goal.Category.LEARNING, Goal.Status.ACTIVE, LocalDate.now().plusMonths(3).toString());
        assertNotNull(goal);
        assertTrue(goal.getId() > 0);
        assertEquals("Master Distributed Systems", goal.getTitle());
        assertEquals(0, goal.getProgress());

        // Update
        goal.setDescription("Updated description");
        service.updateGoal(goal);
        Goal retrieved = service.getGoalById(goal.getId());
        assertEquals("Updated description", retrieved.getDescription());

        // Clean up
        service.deleteGoal(goal.getId());
        assertNull(service.getGoalById(goal.getId()));
    }

    @Test
    @Order(2)
    void testTaskCreationAndAutoProgressCalculation() {
        Goal goal = service.createGoal("Capstone Goal", "Capstone description", Goal.Category.LEARNING, Goal.Status.ACTIVE, "2026-11-30");

        Task t1 = service.createTask(goal.getId(), "Phase 1 - Database", "Setup SQLite", Task.Priority.HIGH, Task.Status.TODO, "2026-11-01");
        Task t2 = service.createTask(goal.getId(), "Phase 2 - UI", "Design JavaFX UI", Task.Priority.HIGH, Task.Status.TODO, "2026-11-05");
        Task t3 = service.createTask(goal.getId(), "Phase 3 - Services", "Connect Services", Task.Priority.MEDIUM, Task.Status.TODO, "2026-11-10");
        Task t4 = service.createTask(goal.getId(), "Phase 4 - Testing", "Write JUnit tests", Task.Priority.MEDIUM, Task.Status.TODO, "2026-11-15");

        // Initially 0%
        assertEquals(0, service.getGoalById(goal.getId()).getProgress());

        // Mark 2 of 4 complete -> 50%
        service.setTaskCompletion(t1.getId(), true);
        service.setTaskCompletion(t2.getId(), true);
        assertEquals(50, service.getGoalById(goal.getId()).getProgress());

        // Mark 3rd complete -> 75%
        service.setTaskCompletion(t3.getId(), true);
        assertEquals(75, service.getGoalById(goal.getId()).getProgress());

        // Unmark 1st task -> back to 50%
        service.setTaskCompletion(t1.getId(), false);
        assertEquals(50, service.getGoalById(goal.getId()).getProgress());

        // Clean up goal (should delete tasks transactionally)
        service.deleteGoal(goal.getId());
        assertNull(service.getGoalById(goal.getId()));
        assertTrue(service.getTasksForGoal(goal.getId()).isEmpty(), "Tasks belonging to deleted goal must be removed");
    }

    @Test
    @Order(3)
    void testHabitTracking() {
        assertThrows(IllegalArgumentException.class, () -> service.createHabit("  ", "desc", Habit.Frequency.DAILY));

        Habit h = service.createHabit("Daily Algorithms Practice", "Solve 1 LeetCode problem", Habit.Frequency.DAILY);
        assertTrue(h.getId() > 0);

        service.toggleHabitCompletion(h.getId(), true);
        List<Habit> habits = service.getAllHabits();
        Habit found = habits.stream().filter(it -> it.getId() == h.getId()).findFirst().orElse(null);
        assertNotNull(found);
        assertTrue(found.isCompletedToday());

        service.toggleHabitCompletion(h.getId(), false);
        habits = service.getAllHabits();
        found = habits.stream().filter(it -> it.getId() == h.getId()).findFirst().orElse(null);
        assertNotNull(found);
        assertFalse(found.isCompletedToday());

        service.deleteHabit(h.getId());
        habits = service.getAllHabits();
        assertFalse(habits.stream().anyMatch(it -> it.getId() == h.getId()));
    }
}
