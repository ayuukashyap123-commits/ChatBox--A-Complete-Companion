package com.chatbox.database;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseTest {

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
    }

    @Test
    @Order(1)
    void testConnectionIsNotNullAndOpen() throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        assertNotNull(conn, "Connection must not be null");
        assertFalse(conn.isClosed(), "Connection must be open");
    }

    @Test
    @Order(2)
    void testTablesCreated() throws SQLException {
        String[] requiredTables = {
            "user_profile", "conversations", "messages", "mood_entries",
            "journal_entries", "goals", "tasks", "habits", "habit_completions",
            "knowledge_documents", "memories"
        };

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            for (String table : requiredTables) {
                try (ResultSet rs = st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
                    assertTrue(rs.next(), "Table " + table + " should exist in SQLite database");
                }
            }
        }
    }

    @Test
    @Order(3)
    void testSeededDataExists() throws SQLException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM user_profile")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0, "Default user profile should be seeded");
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM habits")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0, "Default habits should be seeded");
            }
        }
    }
}
