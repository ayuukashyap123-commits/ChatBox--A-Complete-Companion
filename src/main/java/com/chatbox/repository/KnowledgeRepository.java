package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.KnowledgeDocument;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class KnowledgeRepository {
    private static final Logger LOG = Logger.getLogger(KnowledgeRepository.class.getName());

    public KnowledgeDocument save(KnowledgeDocument doc) {
        String sql = "INSERT INTO knowledge_documents (title, file_path, file_type, content, word_count) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, doc.getTitle());
            ps.setString(2, doc.getFilePath());
            ps.setString(3, doc.getFileType() != null ? doc.getFileType().name() : "OTHER");
            ps.setString(4, doc.getContent());
            ps.setInt(5, doc.getWordCount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) doc.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            LOG.warning("Error saving document: " + e.getMessage());
        }
        return doc;
    }

    public void delete(int id) {
        String sql = "DELETE FROM knowledge_documents WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error deleting document: " + e.getMessage());
        }
    }

    public List<KnowledgeDocument> findAll() {
        List<KnowledgeDocument> list = new ArrayList<>();
        String sql = "SELECT id, title, file_path, file_type, word_count, created_at FROM knowledge_documents ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapDoc(rs, false));
        } catch (SQLException e) {
            LOG.warning("Error loading documents: " + e.getMessage());
        }
        return list;
    }

    public List<KnowledgeDocument> findAllWithContent() {
        List<KnowledgeDocument> list = new ArrayList<>();
        String sql = "SELECT * FROM knowledge_documents ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapDoc(rs, true));
        } catch (SQLException e) {
            LOG.warning("Error loading full documents: " + e.getMessage());
        }
        return list;
    }

    public boolean existsByTitleOrPath(String title, String filePath) {
        String sql = "SELECT COUNT(*) FROM knowledge_documents WHERE title = ? OR (file_path IS NOT NULL AND file_path = ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOG.warning("Error checking document existence: " + e.getMessage());
        }
        return false;
    }

    public KnowledgeDocument findById(int id) {
        String sql = "SELECT * FROM knowledge_documents WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapDoc(rs, true);
            }
        } catch (SQLException e) {
            LOG.warning("Error loading document: " + e.getMessage());
        }
        return null;
    }

    public List<KnowledgeDocument> search(String query) {
        List<KnowledgeDocument> list = new ArrayList<>();
        String sql = "SELECT id, title, file_path, file_type, word_count, created_at FROM knowledge_documents WHERE content LIKE ? OR title LIKE ? ORDER BY created_at DESC";
        String pattern = "%" + query + "%";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapDoc(rs, false));
            }
        } catch (SQLException e) {
            LOG.warning("Error searching documents: " + e.getMessage());
        }
        return list;
    }

    private KnowledgeDocument mapDoc(ResultSet rs, boolean includeContent) throws SQLException {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(rs.getInt("id"));
        doc.setTitle(rs.getString("title"));
        doc.setFilePath(rs.getString("file_path"));
        try { doc.setFileType(KnowledgeDocument.FileType.valueOf(rs.getString("file_type"))); }
        catch (Exception e) { doc.setFileType(KnowledgeDocument.FileType.OTHER); }
        doc.setWordCount(rs.getInt("word_count"));
        if (includeContent) {
            doc.setContent(rs.getString("content"));
        }
        String ca = rs.getString("created_at");
        if (ca != null) doc.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));
        return doc;
    }
}
