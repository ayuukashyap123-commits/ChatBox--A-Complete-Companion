package com.chatbox.service;

import com.chatbox.model.KnowledgeDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles file import, text extraction, validation, and passage retrieval
 * for the Second Brain feature. Supports .txt, .md (Markdown), and .pdf files.
 */
public class DocumentService {
    private static final Logger LOG = Logger.getLogger(DocumentService.class.getName());
    public static final int MAX_CONTENT_CHARS = 500_000;

    public record DocumentPassage(
        String documentTitle,
        KnowledgeDocument.FileType fileType,
        String excerpt,
        int matchIndex,
        double score
    ) {}

    /**
     * Reads and extracts textual content from an input stream.
     * Enables stream-based document ingestion from Android ContentResolver URIs,
     * network/cloud sources, or in-memory test fixtures without requiring local File objects.
     *
     * <p><b>Stream Ownership Rule:</b> The caller retains full responsibility for managing
     * and closing the supplied {@link InputStream}. This method reads all available bytes from
     * the stream via {@link InputStream#readAllBytes()} but does NOT close it, allowing the caller
     * to manage stream lifecycles (e.g. using try-with-resources, re-using pooled streams, or managing
     * Android ContentResolver stream handles). By contrast, {@link #processFile(File)} opens and closes
     * its own {@link FileInputStream} deterministically using a try-with-resources statement.</p>
     *
     * @param fileName the document display name with extension (e.g. "report.pdf", "notes.md")
     * @param inputStream the stream supplying the document bytes (caller retains responsibility for closing)
     * @param sourcePath optional identifier of the source (file path, URI, or URL)
     * @return the extracted and validated {@link KnowledgeDocument}
     * @throws IOException if reading the stream or parsing the document content fails
     */
    public KnowledgeDocument processStream(String fileName, InputStream inputStream, String sourcePath) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Document name cannot be null or blank.");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }

        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Document is empty (0 bytes): " + fileName);
        }

        String name = fileName.trim();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";

        if (!List.of("pdf", "md", "txt").contains(ext)) {
            throw new IllegalArgumentException("Unsupported file format (." + ext + "). Only PDF, Markdown (.md), and Text (.txt) files are supported.");
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name);
        doc.setFilePath(sourcePath != null ? sourcePath : name);

        String content;
        switch (ext) {
            case "pdf" -> {
                doc.setFileType(KnowledgeDocument.FileType.PDF);
                content = extractPdfBytes(bytes, name);
            }
            case "md" -> {
                doc.setFileType(KnowledgeDocument.FileType.MD);
                content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            case "txt" -> {
                doc.setFileType(KnowledgeDocument.FileType.TXT);
                content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            default -> {
                doc.setFileType(KnowledgeDocument.FileType.OTHER);
                content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Document contains no readable text: " + name);
        }

        if (content.length() > MAX_CONTENT_CHARS) {
            content = content.substring(0, MAX_CONTENT_CHARS) + "\n\n[Content truncated — document exceeds maximum size of 500,000 characters]";
        }

        doc.setContent(content);
        doc.setWordCount(countWords(content));
        return doc;
    }

    /**
     * Reads and extracts textual content from a local file.
     * Validates file presence, non-emptiness, format compatibility, and size.
     */
    public KnowledgeDocument processFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File reference cannot be null.");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist: " + file.getAbsolutePath());
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("File is empty (0 bytes): " + file.getName());
        }
        try (InputStream in = new FileInputStream(file)) {
            return processStream(file.getName(), in, file.getAbsolutePath());
        }
    }

    private String extractPdfBytes(byte[] bytes, String fileName) throws IOException {
        try (PDDocument pdfDoc = Loader.loadPDF(bytes)) {
            if (pdfDoc.isEncrypted()) {
                throw new IOException("Cannot read encrypted or password-protected PDF: " + fileName);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDoc);
            return text != null ? text : "";
        } catch (Exception e) {
            if (e instanceof IOException io) throw io;
            throw new IOException("Failed to parse PDF document '" + fileName + "': " + e.getMessage(), e);
        }
    }

    public int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * Simple keyword search within document content.
     * Returns relevant excerpt (up to 500 chars) around first match.
     */
    public String extractRelevantExcerpt(KnowledgeDocument doc, String query) {
        if (doc == null || doc.getContent() == null || query == null || query.isBlank()) return "";
        String lower = doc.getContent().toLowerCase();
        String queryLower = query.toLowerCase();
        int idx = lower.indexOf(queryLower);
        if (idx < 0) return "";
        int start = Math.max(0, idx - 150);
        int end = Math.min(doc.getContent().length(), idx + query.length() + 350);
        String sub = doc.getContent().substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "..." : "") + sub + (end < doc.getContent().length() ? "..." : "");
    }

    /**
     * Search documents for matching passages with scoring and context extraction.
     */
    public List<DocumentPassage> searchPassages(String query, List<KnowledgeDocument> documents, int maxPassages) {
        if (query == null || query.isBlank() || documents == null || documents.isEmpty()) {
            return List.of();
        }

        String cleanQuery = query.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase().trim();
        Set<String> stopWords = Set.of(
            "what", "is", "the", "are", "in", "of", "and", "a", "an", "to", "for", "on", "with",
            "as", "by", "at", "from", "my", "does", "do", "how", "can", "tell", "me", "about",
            "their", "there", "this", "that", "it", "was", "will", "be", "or", "which", "who",
            "notes", "say", "saying"
        );
        List<String> keywords = Arrays.stream(cleanQuery.split("\\s+"))
            .filter(w -> w.length() > 2 && !stopWords.contains(w))
            .toList();

        List<DocumentPassage> matches = new ArrayList<>();
        String lowerQuery = query.trim().toLowerCase();

        for (KnowledgeDocument doc : documents) {
            if (doc.getContent() == null || doc.getContent().isBlank()) continue;
            String content = doc.getContent();
            String lowerContent = content.toLowerCase();

            // 1. Exact phrase match
            int exactIdx = lowerContent.indexOf(lowerQuery);
            if (exactIdx >= 0) {
                String excerpt = buildSnippet(content, exactIdx, lowerQuery.length());
                matches.add(new DocumentPassage(doc.getTitle(), doc.getFileType(), excerpt, exactIdx, 10.0));
                continue;
            }

            // 2. Keyword matches
            int bestIdx = -1;
            int hitCount = 0;
            for (String kw : keywords) {
                int idx = lowerContent.indexOf(kw);
                if (idx >= 0) {
                    hitCount++;
                    if (bestIdx == -1 || idx < bestIdx) bestIdx = idx;
                }
            }

            if (bestIdx >= 0 && hitCount > 0) {
                String excerpt = buildSnippet(content, bestIdx, 20);
                double score = (double) hitCount / Math.max(1, keywords.size());
                matches.add(new DocumentPassage(doc.getTitle(), doc.getFileType(), excerpt, bestIdx, score));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return matches.stream().limit(maxPassages).toList();
    }

    private String buildSnippet(String content, int matchIdx, int matchLen) {
        int start = Math.max(0, matchIdx - 120);
        int end = Math.min(content.length(), matchIdx + matchLen + 200);
        String snippet = content.substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "..." : "") + snippet + (end < content.length() ? "..." : "");
    }
}
