package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Repository for key-value application settings stored in SQLite.
 * Used for persistent AI provider configuration, model preferences,
 * and user choices that survive application restarts.
 */
public class SettingsRepository {
    private static final Logger LOG = Logger.getLogger(SettingsRepository.class.getName());

    public String get(String key, String defaultValue) {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    return val != null ? val : defaultValue;
                }
            }
        } catch (SQLException e) {
            LOG.warning("Failed to read setting '" + key + "': " + e.getMessage());
        }
        return defaultValue;
    }

    public void set(String key, String value) {
        String sql = """
            INSERT INTO app_settings (key, value, updated_at)
            VALUES (?, ?, datetime('now'))
            ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = datetime('now')
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Failed to save setting '" + key + "': " + e.getMessage());
        }
    }

    public void remove(String key) {
        String sql = "DELETE FROM app_settings WHERE key = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Failed to remove setting '" + key + "': " + e.getMessage());
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public void setBoolean(String key, boolean value) {
        set(key, Boolean.toString(value));
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setInt(String key, int value) {
        set(key, Integer.toString(value));
    }
}
