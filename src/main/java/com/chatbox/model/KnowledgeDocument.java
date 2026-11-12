package com.chatbox.model;

import java.time.LocalDateTime;

public class KnowledgeDocument {
    public enum FileType { TXT, MD, PDF, OTHER }

    private int id;
    private String title;
    private String filePath;
    private FileType fileType;
    private String content;
    private int wordCount;
    private LocalDateTime createdAt;

    public KnowledgeDocument() { this.createdAt = LocalDateTime.now(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
