package com.chatbox.service;

import com.chatbox.model.Goal;
import com.chatbox.model.Habit;
import com.chatbox.model.Task;
import com.chatbox.repository.CoachRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service managing Life Coach operations:
 * - Goals with categories, deadlines, and auto-calculated progress percentages.
 * - Tasks linked to goals with priority and completion states.
 * - Daily habit tracking and execution consistency.
 * - Transactional safety and input validation.
 */
public class CoachService {
    private static final Logger LOG = Logger.getLogger(CoachService.class.getName());

    private final CoachRepository repo;

    public CoachService() {
        this(new CoachRepository());
    }

    public CoachService(CoachRepository repo) {
        this.repo = Objects.requireNonNull(repo, "CoachRepository must not be null");
    }

    // ─── Goal Management ──────────────────────────────────────────────────────

    public List<Goal> getAllGoals() {
        return repo.findAllGoals();
    }

    public Goal getGoalById(int id) {
        return repo.findGoalById(id);
    }

    public Goal createGoal(String title, String description, Goal.Category category,
                           Goal.Status status, String targetDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal title cannot be empty.");
        }
        Goal goal = new Goal();
        goal.setTitle(title.trim());
        goal.setDescription(description != null ? description.trim() : "");
        goal.setCategory(category != null ? category : Goal.Category.PERSONAL);
        goal.setStatus(status != null ? status : Goal.Status.ACTIVE);
        goal.setTargetDate(targetDate != null ? targetDate.trim() : "");
        goal.setProgress(0);
        return repo.saveGoal(goal);
    }

    public void updateGoal(Goal goal) {
        if (goal == null || goal.getTitle() == null || goal.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid goal with non-empty title is required.");
        }
        goal.setTitle(goal.getTitle().trim());
        if (goal.getDescription() != null) goal.setDescription(goal.getDescription().trim());
        repo.updateGoal(goal);
    }

    public void deleteGoal(int goalId) {
        // Transactionally deletes goal and any tasks linked to it
        repo.deleteGoal(goalId);
    }

    public int recalculateGoalProgress(int goalId) {
        List<Task> tasks = repo.findTasksByGoalId(goalId);
        if (tasks.isEmpty()) {
            return 0;
        }
        long completed = tasks.stream().filter(t -> t.getStatus() == Task.Status.DONE).count();
        int pct = (int) Math.round((completed * 100.0) / tasks.size());
        repo.updateGoalProgress(goalId, pct);
        return pct;
    }

    // ─── Task Management ──────────────────────────────────────────────────────

    public List<Task> getAllTasks() {
        return repo.findAllTasks();
    }

    public Task getTaskById(int id) {
        return repo.findTaskById(id);
    }

    public List<Task> getTasksForGoal(int goalId) {
        return repo.findTasksByGoalId(goalId);
    }

    public Task createTask(Integer goalId, String title, String description,
                           Task.Priority priority, Task.Status status, String dueDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
        Task task = new Task();
        task.setGoalId(goalId);
        task.setTitle(title.trim());
        task.setDescription(description != null ? description.trim() : "");
        task.setPriority(priority != null ? priority : Task.Priority.MEDIUM);
        task.setStatus(status != null ? status : Task.Status.TODO);
        task.setDueDate(dueDate != null ? dueDate.trim() : "");
        if (task.getStatus() == Task.Status.DONE) {
            task.setCompletedAt(LocalDateTime.now().toString());
        }

        Task saved = repo.saveTask(task);
        if (goalId != null && goalId > 0) {
            recalculateGoalProgress(goalId);
        }
        return saved;
    }

    public void updateTask(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid task with non-empty title is required.");
        }
        task.setTitle(task.getTitle().trim());
        if (task.getDescription() != null) task.setDescription(task.getDescription().trim());
        repo.updateTask(task);
        if (task.getGoalId() != null && task.getGoalId() > 0) {
            recalculateGoalProgress(task.getGoalId());
        }
    }

    public void setTaskCompletion(int taskId, boolean completed) {
        Task task = repo.findTaskById(taskId);
        if (task == null) return;

        task.setStatus(completed ? Task.Status.DONE : Task.Status.TODO);
        task.setCompletedAt(completed ? LocalDateTime.now().toString() : null);
        repo.updateTask(task);

        if (task.getGoalId() != null && task.getGoalId() > 0) {
            recalculateGoalProgress(task.getGoalId());
        }
    }

    public void deleteTask(int taskId) {
        Task task = repo.findTaskById(taskId);
        Integer linkedGoalId = task != null ? task.getGoalId() : null;
        repo.deleteTask(taskId);
        if (linkedGoalId != null && linkedGoalId > 0) {
            recalculateGoalProgress(linkedGoalId);
        }
    }

    public List<Task> getPendingTasks() {
        return repo.findAllTasks().stream()
                .filter(t -> t.getStatus() != Task.Status.DONE)
                .toList();
    }

    // ─── Habit Management ────────────────────────────────────────────────────

    public List<Habit> getAllHabits() {
        return repo.findAllHabits();
    }

    public Habit createHabit(String title, String description, Habit.Frequency frequency) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Habit title cannot be empty.");
        }
        Habit habit = new Habit();
        habit.setTitle(title.trim());
        habit.setDescription(description != null ? description.trim() : "");
        habit.setFrequency(frequency != null ? frequency : Habit.Frequency.DAILY);
        return repo.saveHabit(habit);
    }

    public void toggleHabitCompletion(int habitId, boolean completed) {
        repo.toggleHabitCompletion(habitId, completed);
    }

    public void deleteHabit(int habitId) {
        repo.deleteHabit(habitId);
    }
}
