package com.chatbox.database;

import com.chatbox.service.AppPathService;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection and schema initialization.
 * Database is stored in the user's application data directory.
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private Connection connection;
    private final String dbPath;

    private DatabaseManager() {
        this(AppPathService.getInstance().getDatabasePath());
    }

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath;
        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public String getDbPath() {
        return dbPath;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(true);
            // Enable WAL mode for better concurrency
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
            }
        }
        return connection;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            getConnection();
            createSchema();
            seedDefaultDataIfEmpty();
            LOG.info("Database initialized at: " + dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createSchema() throws SQLException {
        String[] ddl = {
            // User profile
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY,
                name TEXT,
                age INTEGER,
                occupation TEXT,
                interests TEXT,
                communication_style TEXT,
                personality TEXT,
                goals TEXT,
                preferred_name TEXT,
                bio TEXT,
                updated_at TEXT DEFAULT (datetime('now'))
            )""",

            // Conversations
            """
            CREATE TABLE IF NOT EXISTS conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                mode TEXT NOT NULL DEFAULT 'GENERAL',
                created_at TEXT DEFAULT (datetime('now')),
                updated_at TEXT DEFAULT (datetime('now'))
            )""",

            // Messages
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
            )""",

            // Mood check-ins
            """
            CREATE TABLE IF NOT EXISTS mood_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mood_score INTEGER NOT NULL,
                mood_label TEXT,
                note TEXT,
                tags TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )""",

            // Journal entries
            """
            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT NOT NULL,
                mood_score INTEGER,
                tags TEXT,
                created_at TEXT DEFAULT (datetime('now')),
                updated_at TEXT DEFAULT (datetime('now'))
            )""",

            // Goals
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT DEFAULT 'PERSONAL',
                status TEXT DEFAULT 'ACTIVE',
                target_date TEXT,
                progress INTEGER DEFAULT 0,
                created_at TEXT DEFAULT (datetime('now')),
                updated_at TEXT DEFAULT (datetime('now'))
            )""",

            // Tasks
            """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER,
                title TEXT NOT NULL,
                description TEXT,
                priority TEXT DEFAULT 'MEDIUM',
                status TEXT DEFAULT 'TODO',
                due_date TEXT,
                completed_at TEXT,
                created_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE SET NULL
            )""",

            // Habits
            """
            CREATE TABLE IF NOT EXISTS habits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                frequency TEXT DEFAULT 'DAILY',
                created_at TEXT DEFAULT (datetime('now'))
            )""",

            // Habit completions
            """
            CREATE TABLE IF NOT EXISTS habit_completions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                habit_id INTEGER NOT NULL,
                completed_date TEXT NOT NULL,
                FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
                UNIQUE(habit_id, completed_date)
            )""",

            // Knowledge documents
            """
            CREATE TABLE IF NOT EXISTS knowledge_documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                file_path TEXT,
                file_type TEXT,
                content TEXT,
                word_count INTEGER DEFAULT 0,
                created_at TEXT DEFAULT (datetime('now'))
            )""",

            // Memories
            """
            CREATE TABLE IF NOT EXISTS memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                category TEXT DEFAULT 'GENERAL',
                source TEXT DEFAULT 'MANUAL',
                approved INTEGER DEFAULT 1,
                created_at TEXT DEFAULT (datetime('now')),
                updated_at TEXT DEFAULT (datetime('now'))
            )""",

            // Persistent local application settings
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT,
                updated_at TEXT DEFAULT (datetime('now'))
            )"""
        };

        try (Statement st = getConnection().createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        }
    }

    private void seedDefaultDataIfEmpty() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            // Check if profile already exists
            var rs = st.executeQuery("SELECT COUNT(*) FROM user_profile");
            if (rs.next() && rs.getInt(1) > 0) {
                return; // already seeded
            }

            LOG.info("Seeding initial demonstration data for first run...");

            // Default user profile
            st.executeUpdate("""
                INSERT INTO user_profile (id, name, age, occupation, interests, communication_style, personality, goals, preferred_name, bio)
                VALUES (1, 'Ayush Kashyap', 21, 'CS Undergraduate & Software Engineer',
                        'Artificial Intelligence, Native Desktop Systems, Distributed Systems, Cognitive Science',
                        'Structured, analytical, courteous, and detail-oriented',
                        'Curious, introspective, relentless problem-solver',
                        'Graduate with distinction and build impact-driven intelligent software systems',
                        'Ayush',
                        'Senior computer science student developing full-stack AI and desktop companion software. Passionate about human-centric interfaces, private local-first computing, and engineering excellence.')
            """);

            // Default goals
            st.executeUpdate("""
                INSERT INTO goals (id, title, description, category, status, target_date, progress)
                VALUES (1, 'Complete & Deliver Chatbox Companion MVP',
                        'Finalize native JavaFX desktop architecture with SQLite persistence, 6 companion modules, and document intelligence.',
                        'ACADEMIC', 'ACTIVE', date('now', '+14 days'), 85)
            """);

            // Default tasks
            st.executeUpdate("""
                INSERT INTO tasks (goal_id, title, description, priority, status, due_date)
                VALUES (1, 'Architect SQLite persistence layer with WAL mode',
                        'Implement SQLite schema, foreign keys, and JDBC repositories', 'HIGH', 'DONE', date('now', '-2 days')),
                       (1, 'Design modern dark charcoal/violet JavaFX UI',
                        'Create complete stylesheet with tokens, animations, and custom controls', 'HIGH', 'DONE', date('now', '-1 day')),
                       (1, 'Integrate Apache PDFBox 3.x document parsing',
                        'Support PDF, TXT, and Markdown document import in Second Brain', 'MEDIUM', 'DONE', date('now')),
                       (1, 'Prepare comprehensive project report and defense demo',
                        'Demonstrate offline fallback, real Gemini AI, and all 6 companion modules', 'HIGH', 'TODO', date('now', '+7 days'))
            """);

            // Default habits
            st.executeUpdate("""
                INSERT INTO habits (id, title, description, frequency)
                VALUES (1, 'Deep Work Session (45m)', 'Focus without digital distractions on core engineering tasks', 'DAILY'),
                       (2, 'Read Technical Papers or Docs', 'Stay updated on modern system design and AI research', 'DAILY'),
                       (3, 'Evening Mindful Reflection & Journaling', 'Check in on mental state and review daily accomplishments', 'DAILY')
            """);

            // Habit completions for today
            st.executeUpdate("""
                INSERT OR IGNORE INTO habit_completions (habit_id, completed_date)
                VALUES (1, date('now')), (2, date('now'))
            """);

            // Mood entries
            st.executeUpdate("""
                INSERT INTO mood_entries (mood_score, mood_label, note, tags, created_at)
                VALUES (4, 'Productive & Focused', 'Architecture settled and compilation successful. Feeling confident about the presentation.', 'focus, milestone', datetime('now', '-1 day')),
                       (5, 'Energized & Motivated', 'All six companion modules connected and styled cleanly.', 'excited, flow, achievement', datetime('now'))
            """);

            // Journal entry
            st.executeUpdate("""
                INSERT INTO journal_entries (title, content, mood_score, tags, created_at)
                VALUES ('Building Chatbox Companion',
                        'Made tremendous progress designing an offline-first companion application with JavaFX and SQLite. The modular architecture separates concerns cleanly and the dark theme looks professional.',
                        5, 'reflection, engineering', datetime('now'))
            """);

            // Default memories
            st.executeUpdate("""
                INSERT INTO memories (content, category, source, approved)
                VALUES ('Prefers concise, structured, bulleted explanations with clean code examples.', 'PREFERENCE', 'MANUAL', 1),
                       ('Currently completing final year software engineering capstone on AI companion software.', 'BACKGROUND', 'MANUAL', 1),
                       ('Values privacy, offline-first local persistence, and native desktop responsiveness.', 'VALUE', 'MANUAL', 1)
            """);

            // Default conversation and welcome message
            st.executeUpdate("""
                INSERT INTO conversations (id, title, mode, created_at)
                VALUES (1, 'Getting Started with Chatbox', 'GENERAL', datetime('now'))
            """);
            st.executeUpdate("""
                INSERT INTO messages (conversation_id, role, content, created_at)
                VALUES (1, 'ASSISTANT',
                        'Welcome to Chatbox — A Complete Companion!\n\nI am your personalized AI assistant, life coach, second brain, and mental wellness companion. All your data stays safely stored on your local machine in SQLite. How can I assist you today?',
                        datetime('now'))
            """);

        } catch (SQLException e) {
            LOG.warning("Error seeding default data: " + e.getMessage());
        }
    }

    /**
     * Safely queries record counts for system tables without exposing raw SQL in UI controllers.
     * Uses a whitelist of allowed table names to ensure SQL injection safety.
     */
    public Map<String, Integer> getTableRecordCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String[] tables = {
            "conversations", "messages", "goals", "tasks",
            "habits", "mood_entries", "journal_entries", "knowledge_documents", "memories", "app_settings"
        };
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            for (String table : tables) {
                int count = 0;
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                } catch (SQLException ignored) {}
                counts.put(table, count);
            }
        } catch (SQLException e) {
            LOG.warning("Failed to query table record counts: " + e.getMessage());
        }
        return counts;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOG.warning("Error closing database: " + e.getMessage());
        }
    }
}
