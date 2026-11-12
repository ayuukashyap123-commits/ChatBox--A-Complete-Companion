package com.chatbox.service;

import com.chatbox.model.KnowledgeDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentServiceTest {

    private final DocumentService documentService = new DocumentService();

    @Test
    void testProcessTxtFile(@TempDir Path tempDir) throws IOException {
        Path textFile = tempDir.resolve("notes.txt");
        String text = "JavaFX and SQLite make an excellent combination for desktop software.\n" +
                      "This document tests the Second Brain file processing logic.";
        Files.writeString(textFile, text);

        KnowledgeDocument doc = documentService.processFile(textFile.toFile());
        assertNotNull(doc);
        assertEquals("notes", doc.getTitle());
        assertEquals(KnowledgeDocument.FileType.TXT, doc.getFileType());
        assertTrue(doc.getContent().contains("Second Brain"));
        assertEquals(19, doc.getWordCount());
    }

    @Test
    void testProcessMarkdownFile(@TempDir Path tempDir) throws IOException {
        Path mdFile = tempDir.resolve("project.md");
        String content = "# Chatbox Companion\n\n- Feature 1: AI Chat\n- Feature 2: Life Coach\n- Feature 3: Second Brain";
        Files.writeString(mdFile, content);

        KnowledgeDocument doc = documentService.processFile(mdFile.toFile());
        assertNotNull(doc);
        assertEquals("project", doc.getTitle());
        assertEquals(KnowledgeDocument.FileType.MD, doc.getFileType());
        assertTrue(doc.getContent().contains("Chatbox Companion"));
        assertTrue(doc.getWordCount() > 0);
    }

    @Test
    void testExtractRelevantExcerpt() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setContent("Artificial Intelligence companions are designed to offer personalized emotional and productivity support across everyday workflows.");

        String excerpt = documentService.extractRelevantExcerpt(doc, "productivity");
        assertNotNull(excerpt);
        assertTrue(excerpt.contains("productivity"), "Excerpt should contain the query keyword");

        String notFound = documentService.extractRelevantExcerpt(doc, "nonexistentkeyword");
        assertEquals("", notFound);
    }

    @Test
    void testProcessPdfFile(@TempDir Path tempDir) throws IOException {
        File pdfFile = tempDir.resolve("test-document.pdf").toFile();

        // Create a minimal PDF using PDFBox 3.x
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Chatbox Second Brain PDF Extraction Verified.");
                stream.endText();
            }
            pdf.save(pdfFile);
        }

        assertTrue(pdfFile.exists());

        KnowledgeDocument doc = documentService.processFile(pdfFile);
        assertNotNull(doc);
        assertEquals("test-document", doc.getTitle());
        assertEquals(KnowledgeDocument.FileType.PDF, doc.getFileType());
        assertTrue(doc.getContent().contains("Chatbox Second Brain PDF Extraction Verified."));
    }
}
