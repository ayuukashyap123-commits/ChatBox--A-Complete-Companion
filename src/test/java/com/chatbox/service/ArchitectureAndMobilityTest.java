package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Conversation;
import com.chatbox.model.KnowledgeDocument;
import com.chatbox.model.Message;
import com.chatbox.repository.ConversationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArchitectureAndMobilityTest {

    private AppPathService originalPathService;

    @BeforeEach
    void setUp() {
        DatabaseManager.getInstance().initialize();
        originalPathService = AppPathService.getInstance();
    }

    @AfterEach
    void tearDown() {
        AppPathService.setInstance(originalPathService);
    }

    @Test
    @DisplayName("AppPathService provides desktop default paths and supports custom platform injection")
    void testAppPathService() {
        AppPathService service = AppPathService.getInstance();
        assertNotNull(service, "Default AppPathService must not be null");
        assertNotNull(service.getAppDataDirectory());
        assertNotNull(service.getDatabasePath());
        assertTrue(service.getDatabasePath().endsWith("chatbox.db"));

        // Test custom path injection (simulating future Android context.getFilesDir())
        AppPathService mockAndroidPathService = new AppPathService() {
            @Override
            public String getAppDataDirectory() {
                return "/data/user/0/com.chatbox.mobile/files";
            }

            @Override
            public String getDatabasePath() {
                return "/data/user/0/com.chatbox.mobile/databases/chatbox.db";
            }
        };

        AppPathService.setInstance(mockAndroidPathService);
        assertEquals("/data/user/0/com.chatbox.mobile/files", AppPathService.getInstance().getAppDataDirectory());
        assertEquals("/data/user/0/com.chatbox.mobile/databases/chatbox.db", AppPathService.getInstance().getDatabasePath());
    }

    @Test
    @DisplayName("DesktopPathService creates .chatbox folder and resolves chatbox.db")
    void testDesktopPathService() {
        String tempHome = System.getProperty("java.io.tmpdir") + File.separator + "chatbox_test_home_" + System.currentTimeMillis();
        DesktopPathService desktopService = new DesktopPathService(tempHome);

        assertTrue(desktopService.getAppDataDirectory().contains(".chatbox"));
        assertTrue(desktopService.getDatabasePath().contains("chatbox.db"));
        assertTrue(new File(desktopService.getAppDataDirectory()).exists(), "Directory should be created");

        // Cleanup
        new File(desktopService.getAppDataDirectory()).delete();
    }

    @Test
    @DisplayName("DatabaseManager returns storage record counts safely without raw SQL in UI")
    void testGetTableRecordCounts() {
        Map<String, Integer> counts = DatabaseManager.getInstance().getTableRecordCounts();
        assertNotNull(counts);
        assertTrue(counts.containsKey("conversations"));
        assertTrue(counts.containsKey("messages"));
        assertTrue(counts.containsKey("goals"));
        assertTrue(counts.containsKey("tasks"));
        assertTrue(counts.containsKey("habits"));
        assertTrue(counts.containsKey("mood_entries"));
        assertTrue(counts.containsKey("journal_entries"));
        assertTrue(counts.containsKey("knowledge_documents"));
        assertTrue(counts.containsKey("memories"));
        assertTrue(counts.containsKey("app_settings"));

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            assertTrue(entry.getValue() >= 0, "Record count for " + entry.getKey() + " must be non-negative");
        }
    }

    @Test
    @DisplayName("DocumentService processes document from InputStream without local File dependency")
    void testDocumentServiceProcessStream() throws IOException {
        DocumentService docService = new DocumentService();

        String mdContent = "# System Architecture\n\nChatbox is an offline-first companion application with local SQLite storage.";
        ByteArrayInputStream in = new ByteArrayInputStream(mdContent.getBytes(StandardCharsets.UTF_8));

        KnowledgeDocument doc = docService.processStream("architecture.md", in, "content://com.chatbox/docs/1");

        assertNotNull(doc);
        assertEquals("architecture", doc.getTitle());
        assertEquals(KnowledgeDocument.FileType.MD, doc.getFileType());
        assertEquals("content://com.chatbox/docs/1", doc.getFilePath());
        assertTrue(doc.getContent().contains("System Architecture"));
        assertTrue(doc.getWordCount() > 0);

        // Test search within stream-parsed document
        List<DocumentService.DocumentPassage> passages = docService.searchPassages("SQLite", List.of(doc), 5);
        assertFalse(passages.isEmpty(), "Passage search should find match in stream-processed document");
        assertEquals("architecture", passages.get(0).documentTitle());
    }

    @Test
    @DisplayName("DocumentService handles stream validation errors properly")
    void testDocumentServiceStreamValidation() {
        DocumentService docService = new DocumentService();

        // Null name
        assertThrows(IllegalArgumentException.class, () ->
            docService.processStream(null, new ByteArrayInputStream(new byte[]{1}), "test"));

        // Null stream
        assertThrows(IllegalArgumentException.class, () ->
            docService.processStream("doc.txt", null, "test"));

        // Empty stream
        assertThrows(IllegalArgumentException.class, () ->
            docService.processStream("doc.txt", new ByteArrayInputStream(new byte[0]), "test"));

        // Unsupported format
        assertThrows(IllegalArgumentException.class, () ->
            docService.processStream("image.png", new ByteArrayInputStream(new byte[]{1, 2, 3}), "test"));
    }

    @Test
    @DisplayName("ConversationRepository provides transactional deleteAll")
    void testConversationRepositoryDeleteAll() {
        ConversationRepository repo = new ConversationRepository();

        // Save a test conversation
        Conversation conv = new Conversation();
        conv.setTitle("Temporary Architecture Test");
        conv.setMode(Conversation.Mode.GENERAL);
        repo.save(conv);

        Message msg = new Message();
        msg.setConversationId(conv.getId());
        msg.setRole(Message.Role.USER);
        msg.setContent("Test message");
        repo.saveMessage(msg);

        assertTrue(repo.findById(conv.getId()) != null);

        // Backup existing conversations before deleteAll
        List<Conversation> existing = repo.findAll();
        assertFalse(existing.isEmpty());

        // Perform deleteAll
        repo.deleteAll();
        assertEquals(0, repo.findAll().size(), "Conversations should be empty after deleteAll");

        // Restore default seeded conversation so overall app state remains ready
        Conversation defaultConv = new Conversation();
        defaultConv.setTitle("Getting Started with Chatbox");
        defaultConv.setMode(Conversation.Mode.GENERAL);
        repo.save(defaultConv);

        Message welcomeMsg = new Message();
        welcomeMsg.setConversationId(defaultConv.getId());
        welcomeMsg.setRole(Message.Role.ASSISTANT);
        welcomeMsg.setContent("Welcome to Chatbox — A Complete Companion!");
        repo.saveMessage(welcomeMsg);
    }
}
