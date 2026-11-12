package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.MoodEntry;
import com.chatbox.model.JournalEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WellnessRepository {
    private static final Logger LOG = Logger.getLogger(WellnessRepository.class.getName());

    // ─── Mood entries ───────────────────────────────────────────────────────────

    public MoodEntry saveMood(MoodEntry entry) {
        String sql = "INSERT INTO mood_entries (mood_score, mood_label, note, tags) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getMoodScore());
            ps.setString(2, entry.getMoodLabel());
            ps.setString(3, entry.getNote());
            ps.setString(4, entry.getTags());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving mood: " + e.getMessage());
        }
        return entry;
    }

    public List<MoodEntry> findRecentMoods(int limit) {
        List<MoodEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM mood_entries ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMood(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Error loading moods: " + e.getMessage());
        }
        return list;
    }

    public MoodEntry findMoodById(int id) {
        String sql = "SELECT * FROM mood_entries WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMood(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error finding mood: " + e.getMessage());
        }
        return null;
    }

    public List<MoodEntry> findAllMoods() {
        List<MoodEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM mood_entries ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapMood(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading moods: " + e.getMessage());
        }
        return list;
    }

    // ─── Journal entries ─────────────────────────────────────────────────────

    public JournalEntry saveJournal(JournalEntry entry) {
        String sql = "INSERT INTO journal_entries (title, content, mood_score, tags) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getContent());
            ps.setInt(3, entry.getMoodScore());
            ps.setString(4, entry.getTags());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving journal: " + e.getMessage());
        }
        return entry;
    }

    public void updateJournal(JournalEntry entry) {
        String sql = """
            UPDATE journal_entries
            SET title=?, content=?, mood_score=?, tags=?, updated_at=datetime('now')
            WHERE id=?""";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getContent());
            ps.setInt(3, entry.getMoodScore());
            ps.setString(4, entry.getTags());
            ps.setInt(5, entry.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating journal: " + e.getMessage());
        }
    }

    public void deleteJournal(int id) {
        String sql = "DELETE FROM journal_entries WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error deleting journal: " + e.getMessage());
        }
    }

    public JournalEntry findJournalById(int id) {
        String sql = "SELECT * FROM journal_entries WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapJournal(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error finding journal by id: " + e.getMessage());
        }
        return null;
    }

    public List<JournalEntry> findAllJournals() {
        List<JournalEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM journal_entries ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapJournal(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading journals: " + e.getMessage());
        }
        return list;
    }

    private MoodEntry mapMood(ResultSet rs) throws SQLException {
        MoodEntry m = new MoodEntry();
        m.setId(rs.getInt("id"));
        m.setMoodScore(rs.getInt("mood_score"));
        m.setMoodLabel(rs.getString("mood_label"));
        m.setNote(rs.getString("note"));
        m.setTags(rs.getString("tags"));
        String ca = rs.getString("created_at");
        if (ca != null) m.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return m;
    }

    private JournalEntry mapJournal(ResultSet rs) throws SQLException {
        JournalEntry j = new JournalEntry();
        j.setId(rs.getInt("id"));
        j.setTitle(rs.getString("title"));
        j.setContent(rs.getString("content"));
        j.setMoodScore(rs.getInt("mood_score"));
        j.setTags(rs.getString("tags"));
        String ca = rs.getString("created_at");
        if (ca != null) j.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        String ua = rs.getString("updated_at");
        if (ua != null) j.setUpdatedAt(LocalDateTime.parse(ua.replace(" ", "T")));
        return j;
    }
}
