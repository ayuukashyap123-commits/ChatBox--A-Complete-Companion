package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Goal;
import com.chatbox.model.Task;
import com.chatbox.model.Habit;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CoachRepository {
    private static final Logger LOG = Logger.getLogger(CoachRepository.class.getName());

    // ─── Goals ───────────────────────────────────────────────────────────────

    public Goal saveGoal(Goal goal) {
        String sql = "INSERT INTO goals (title, description, category, status, target_date, progress) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, goal.getTitle());
            ps.setString(2, goal.getDescription());
            ps.setString(3, goal.getCategory().name());
            ps.setString(4, goal.getStatus().name());
            ps.setString(5, goal.getTargetDate());
            ps.setInt(6, goal.getProgress());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) goal.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving goal: " + e.getMessage());
        }
        return goal;
    }

    public void updateGoal(Goal goal) {
        String sql = """
            UPDATE goals SET title=?, description=?, category=?, status=?,
            target_date=?, progress=?, updated_at=datetime('now') WHERE id=?""";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getTitle());
            ps.setString(2, goal.getDescription());
            ps.setString(3, goal.getCategory().name());
            ps.setString(4, goal.getStatus().name());
            ps.setString(5, goal.getTargetDate());
            ps.setInt(6, goal.getProgress());
            ps.setInt(7, goal.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating goal: " + e.getMessage());
        }
    }

    public Goal findGoalById(int id) {
        String sql = "SELECT * FROM goals WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapGoal(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error loading goal by id: " + e.getMessage());
        }
        return null;
    }

    public void updateGoalProgress(int goalId, int progress) {
        String sql = "UPDATE goals SET progress=?, updated_at=datetime('now') WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, Math.min(100, progress)));
            ps.setInt(2, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating goal progress: " + e.getMessage());
        }
    }

    public void deleteGoal(int id) {
        String deleteTasksSql = "DELETE FROM tasks WHERE goal_id=?";
        String deleteGoalSql = "DELETE FROM goals WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            boolean auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement psTasks = conn.prepareStatement(deleteTasksSql);
                 PreparedStatement psGoal = conn.prepareStatement(deleteGoalSql)) {
                psTasks.setInt(1, id);
                psTasks.executeUpdate();
                psGoal.setInt(1, id);
                psGoal.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            LOG.warning("Error deleting goal and tasks transactionally: " + e.getMessage());
        }
    }

    public List<Goal> findAllGoals() {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals ORDER BY status ASC, updated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapGoal(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading goals: " + e.getMessage());
        }
        return list;
    }

    // ─── Tasks ───────────────────────────────────────────────────────────────

    public Task saveTask(Task task) {
        String sql = "INSERT INTO tasks (goal_id, title, description, priority, status, due_date) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (task.getGoalId() != null) ps.setInt(1, task.getGoalId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getStatus().name());
            ps.setString(6, task.getDueDate());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) task.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving task: " + e.getMessage());
        }
        return task;
    }

    public void updateTask(Task task) {
        String sql = """
            UPDATE tasks SET goal_id=?, title=?, description=?, priority=?,
            status=?, due_date=?, completed_at=? WHERE id=?""";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (task.getGoalId() != null) ps.setInt(1, task.getGoalId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getPriority().name());
            ps.setString(5, task.getStatus().name());
            ps.setString(6, task.getDueDate());
            ps.setString(7, task.getCompletedAt());
            ps.setInt(8, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating task: " + e.getMessage());
        }
    }

    public void deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error deleting task: " + e.getMessage());
        }
    }

    public Task findTaskById(int id) {
        String sql = "SELECT * FROM tasks WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTask(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error finding task by id: " + e.getMessage());
        }
        return null;
    }

    public List<Task> findTasksByGoalId(int goalId) {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE goal_id=? ORDER BY status ASC, priority DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTask(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Error finding tasks by goal id: " + e.getMessage());
        }
        return list;
    }

    public List<Task> findAllTasks() {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY status ASC, priority DESC, created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapTask(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading tasks: " + e.getMessage());
        }
        return list;
    }

    // ─── Habits ──────────────────────────────────────────────────────────────

    public Habit saveHabit(Habit habit) {
        String sql = "INSERT INTO habits (title, description, frequency) VALUES (?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, habit.getTitle());
            ps.setString(2, habit.getDescription());
            ps.setString(3, habit.getFrequency().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) habit.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving habit: " + e.getMessage());
        }
        return habit;
    }

    public void deleteHabit(int id) {
        String delCompSql = "DELETE FROM habit_completions WHERE habit_id=?";
        String delHabitSql = "DELETE FROM habits WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            boolean auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(delCompSql);
                 PreparedStatement ps2 = conn.prepareStatement(delHabitSql)) {
                ps1.setInt(1, id);
                ps1.executeUpdate();
                ps2.setInt(1, id);
                ps2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            LOG.warning("Error deleting habit transactionally: " + e.getMessage());
        }
    }

    public void toggleHabitCompletion(int habitId, boolean completed) {
        String today = LocalDate.now().toString();
        if (completed) {
            String sql = "INSERT OR IGNORE INTO habit_completions (habit_id, completed_date) VALUES (?,?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, habitId);
                ps.setString(2, today);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOG.warning("Error marking habit: " + e.getMessage());
            }
        } else {
            String sql = "DELETE FROM habit_completions WHERE habit_id=? AND completed_date=?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, habitId);
                ps.setString(2, today);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOG.warning("Error unmarking habit: " + e.getMessage());
            }
        }
    }

    public List<Habit> findAllHabits() {
        List<Habit> list = new ArrayList<>();
        String today = LocalDate.now().toString();
        String sql = """
            SELECT h.*,
                (SELECT COUNT(*) FROM habit_completions hc WHERE hc.habit_id=h.id AND hc.completed_date=?) as done_today
            FROM habits h ORDER BY h.created_at ASC""";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Habit h = mapHabit(rs);
                    h.setCompletedToday(rs.getInt("done_today") > 0);
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            LOG.warning("Error loading habits: " + e.getMessage());
        }
        return list;
    }

    private Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        try { g.setCategory(Goal.Category.valueOf(rs.getString("category"))); } catch (Exception e) { g.setCategory(Goal.Category.PERSONAL); }
        try { g.setStatus(Goal.Status.valueOf(rs.getString("status"))); } catch (Exception e) { g.setStatus(Goal.Status.ACTIVE); }
        g.setTargetDate(rs.getString("target_date"));
        g.setProgress(rs.getInt("progress"));
        String ca = rs.getString("created_at");
        if (ca != null) g.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return g;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        int goalId = rs.getInt("goal_id");
        if (!rs.wasNull()) t.setGoalId(goalId);
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        try { t.setPriority(Task.Priority.valueOf(rs.getString("priority"))); } catch (Exception e) { t.setPriority(Task.Priority.MEDIUM); }
        try { t.setStatus(Task.Status.valueOf(rs.getString("status"))); } catch (Exception e) { t.setStatus(Task.Status.TODO); }
        t.setDueDate(rs.getString("due_date"));
        t.setCompletedAt(rs.getString("completed_at"));
        String ca = rs.getString("created_at");
        if (ca != null) t.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return t;
    }

    private Habit mapHabit(ResultSet rs) throws SQLException {
        Habit h = new Habit();
        h.setId(rs.getInt("id"));
        h.setTitle(rs.getString("title"));
        h.setDescription(rs.getString("description"));
        try { h.setFrequency(Habit.Frequency.valueOf(rs.getString("frequency"))); } catch (Exception e) { h.setFrequency(Habit.Frequency.DAILY); }
        String ca = rs.getString("created_at");
        if (ca != null) h.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return h;
    }
}
