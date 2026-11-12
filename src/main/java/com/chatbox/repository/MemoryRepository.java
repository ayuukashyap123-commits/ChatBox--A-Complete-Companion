package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Memory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MemoryRepository {
    private static final Logger LOG = Logger.getLogger(MemoryRepository.class.getName());

    public Memory save(Memory memory) {
        String sql = "INSERT INTO memories (content, category, source, approved) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, memory.getContent());
            ps.setString(2, memory.getCategory().name());
            ps.setString(3, memory.getSource());
            ps.setInt(4, memory.isApproved() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) memory.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving memory: " + e.getMessage());
        }
        return memory;
    }

    public void update(Memory memory) {
        String sql = "UPDATE memories SET content=?, category=?, approved=?, updated_at=datetime('now') WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memory.getContent());
            ps.setString(2, memory.getCategory().name());
            ps.setInt(3, memory.isApproved() ? 1 : 0);
            ps.setInt(4, memory.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating memory: " + e.getMessage());
        }
    }

    public Memory findById(int id) {
        String sql = "SELECT * FROM memories WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMemory(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error finding memory by id: " + e.getMessage());
        }
        return null;
    }

    public void setApproved(int id, boolean approved) {
        String sql = "UPDATE memories SET approved=?, updated_at=datetime('now') WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approved ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error setting memory approval: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM memories WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error deleting memory: " + e.getMessage());
        }
    }

    public List<Memory> findAll() {
        List<Memory> list = new ArrayList<>();
        String sql = "SELECT * FROM memories ORDER BY updated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapMemory(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading memories: " + e.getMessage());
        }
        return list;
    }

    public List<Memory> findApproved() {
        List<Memory> list = new ArrayList<>();
        String sql = "SELECT * FROM memories WHERE approved=1 ORDER BY updated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapMemory(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading approved memories: " + e.getMessage());
        }
        return list;
    }

    private Memory mapMemory(ResultSet rs) throws SQLException {
        Memory m = new Memory();
        m.setId(rs.getInt("id"));
        m.setContent(rs.getString("content"));
        try { m.setCategory(Memory.Category.valueOf(rs.getString("category"))); }
        catch (Exception e) { m.setCategory(Memory.Category.GENERAL); }
        m.setSource(rs.getString("source"));
        m.setApproved(rs.getInt("approved") == 1);
        String ca = rs.getString("created_at");
        if (ca != null) m.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        String ua = rs.getString("updated_at");
        if (ua != null) m.setUpdatedAt(LocalDateTime.parse(ua.replace(" ", "T")));
        return m;
    }
}
