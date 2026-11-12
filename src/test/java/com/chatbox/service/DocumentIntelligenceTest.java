package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Conversation;
import com.chatbox.model.KnowledgeDocument;
import com.chatbox.model.UserProfile;
import com.chatbox.repository.KnowledgeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentIntelligenceTest {

    private DocumentService documentService;
    private KnowledgeRepository knowledgeRepo;
    private AiService aiService;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        DatabaseManager.getInstance().initialize();
        documentService = new DocumentService();
        knowledgeRepo = new KnowledgeRepository();
        aiService = new AiService();
        aiService.getConfig().setForceOffline(true);
        testProfile = new UserProfile();
        testProfile.setName("Ayush Kashyap");
        testProfile.setPreferredName("Ayush");
    }

    @AfterEach
    void cleanUp() {
        // Clean any test documents
        for (KnowledgeDocument d : knowledgeRepo.findAll()) {
            if (d.getTitle().startsWith("TestDoc_")) {
                knowledgeRepo.delete(d.getId());
            }
        }
    }

    @Test
    void testEmptyFileRejection(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            documentService.processFile(emptyFile.toFile())
        );
        assertTrue(ex.getMessage().contains("empty (0 bytes)"));
    }

    @Test
    void testUnsupportedFormatRejection(@TempDir Path tempDir) throws IOException {
        Path invalidFile = tempDir.resolve("script.py");
        Files.writeString(invalidFile, "print('hello')");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            documentService.processFile(invalidFile.toFile())
        );
        assertTrue(ex.getMessage().contains("Unsupported file format"));
    }

    @Test
    void testCorruptPdfHandling(@TempDir Path tempDir) throws IOException {
        Path corruptPdf = tempDir.resolve("corrupt.pdf");
        Files.writeString(corruptPdf, "This is not a real PDF document content header");

        IOException ex = assertThrows(IOException.class, () ->
            documentService.processFile(corruptPdf.toFile())
        );
        assertNotNull(ex.getMessage());
    }

    @Test
    void testPassageSearchExactMatchAndRanking() {
        KnowledgeDocument doc1 = new KnowledgeDocument();
        doc1.setTitle("Distributed Systems Notes");
        doc1.setFileType(KnowledgeDocument.FileType.MD);
        doc1.setContent("The CAP theorem specifies that a distributed data store can guarantee at most two out of consistency, availability, and partition tolerance.");

        KnowledgeDocument doc2 = new KnowledgeDocument();
        doc2.setTitle("Web Development Guide");
        doc2.setFileType(KnowledgeDocument.FileType.TXT);
        doc2.setContent("Cascading Style Sheets (CSS) format the layout and visual presentation of web elements.");

        List<DocumentService.DocumentPassage> passages = documentService.searchPassages(
            "CAP theorem consistency availability",
            List.of(doc1, doc2),
            3
        );

        assertFalse(passages.isEmpty());
        assertEquals("Distributed Systems Notes", passages.get(0).documentTitle());
        assertTrue(passages.get(0).excerpt().contains("CAP theorem"));
    }

    @Test
    void testGroundedKnowledgeAnswerWhenPassageFound() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle("TestDoc_Architecture");
        doc.setFilePath("C:/docs/arch.md");
        doc.setFileType(KnowledgeDocument.FileType.MD);
        doc.setContent("The Chatbox Companion employs an offline-first architecture utilizing SQLite with WAL mode for zero external data dependencies.");
        doc.setWordCount(22);
        knowledgeRepo.save(doc);

        String answer = aiService.chat(
            Conversation.Mode.KNOWLEDGE,
            "What architecture does Chatbox use?",
            List.of(),
            testProfile
        );

        assertNotNull(answer);
        assertTrue(answer.contains("TestDoc_Architecture"));
        assertTrue(answer.contains("offline-first architecture"));
    }

    @Test
    void testInsufficientEvidenceWhenNoDocumentsMatch() {
        String answer = aiService.chat(
            Conversation.Mode.KNOWLEDGE,
            "NonExistentTopicRandomQueryZyx999",
            List.of(),
            testProfile
        );

        assertNotNull(answer);
        assertTrue(answer.contains("Insufficient Evidence"));
        assertTrue(answer.contains("The imported documents do not contain enough evidence to answer this question."));
        // Guarantee: No fabricated citations
        assertFalse(answer.contains("Page 42"));
        assertFalse(answer.contains("According to chapter 3"));
    }

    @Test
    void testDuplicateImportPrevention() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle("TestDoc_UniqueModule");
        doc.setFilePath("C:/docs/unique.txt");
        doc.setFileType(KnowledgeDocument.FileType.TXT);
        doc.setContent("Unique content for duplicate checking.");
        doc.setWordCount(5);
        knowledgeRepo.save(doc);

        assertTrue(knowledgeRepo.existsByTitleOrPath("TestDoc_UniqueModule", "C:/docs/unique.txt"));
        assertFalse(knowledgeRepo.existsByTitleOrPath("CompletelyDifferentTitle", "C:/docs/other.txt"));
    }
}
