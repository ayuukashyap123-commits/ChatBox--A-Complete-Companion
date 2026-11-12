package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Conversation;
import com.chatbox.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ConversationRepository {
    private static final Logger LOG = Logger.getLogger(ConversationRepository.class.getName());

    public Conversation save(Conversation conv) {
        String sql = "INSERT INTO conversations (title, mode) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, conv.getTitle());
            ps.setString(2, conv.getMode().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) conv.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving conversation: " + e.getMessage());
        }
        return conv;
    }

    public void updateTitle(int id, String title) {
        String sql = "UPDATE conversations SET title=?, updated_at=datetime('now') WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error updating conversation: " + e.getMessage());
        }
    }

    public Conversation findById(int id) {
        String sql = "SELECT * FROM conversations WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapConversation(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error finding conversation: " + e.getMessage());
        }
        return null;
    }

    public void delete(int id) {
        String delMsgs = "DELETE FROM messages WHERE conversation_id=?";
        String delConv = "DELETE FROM conversations WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            boolean auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(delMsgs);
                 PreparedStatement ps2 = conn.prepareStatement(delConv)) {
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
            LOG.warning("Error deleting conversation transactionally: " + e.getMessage());
        }
    }

    /**
     * Transactionally clears all conversations and associated messages.
     * Keeps database operations encapsulated in the repository layer rather than controllers.
     */
    public void deleteAll() {
        String delMsgs = "DELETE FROM messages";
        String delConv = "DELETE FROM conversations";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            boolean auto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(delMsgs);
                st.executeUpdate(delConv);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            LOG.warning("Error clearing all conversations transactionally: " + e.getMessage());
        }
    }

    public List<Conversation> findAll() {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT * FROM conversations ORDER BY updated_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapConversation(rs));
        } catch (SQLException e) {
            LOG.warning("Error loading conversations: " + e.getMessage());
        }
        return list;
    }

    public Message saveMessage(Message msg) {
        String sql = "INSERT INTO messages (conversation_id, role, content) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getConversationId());
            ps.setString(2, msg.getRole().name());
            ps.setString(3, msg.getContent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) msg.setId(keys.getInt(1));
            }
            // Touch conversation updated_at
            String upd = "UPDATE conversations SET updated_at=datetime('now') WHERE id=?";
            try (PreparedStatement ps2 = conn.prepareStatement(upd)) {
                ps2.setInt(1, msg.getConversationId());
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.warning("Error saving message: " + e.getMessage());
        }
        return msg;
    }

    public List<Message> findMessages(int conversationId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id=? ORDER BY created_at ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            LOG.warning("Error loading messages: " + e.getMessage());
        }
        return list;
    }

    private Conversation mapConversation(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        try { c.setMode(Conversation.Mode.valueOf(rs.getString("mode"))); }
        catch (Exception e) { c.setMode(Conversation.Mode.GENERAL); }
        String ca = rs.getString("created_at");
        if (ca != null) c.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        String ua = rs.getString("updated_at");
        if (ua != null) c.setUpdatedAt(LocalDateTime.parse(ua.replace(" ", "T")));
        return c;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setConversationId(rs.getInt("conversation_id"));
        try { m.setRole(Message.Role.valueOf(rs.getString("role"))); }
        catch (Exception e) { m.setRole(Message.Role.USER); }
        m.setContent(rs.getString("content"));
        String ca = rs.getString("created_at");
        if (ca != null) m.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return m;
    }
}
