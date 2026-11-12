package com.chatbox.model;

import java.time.LocalDateTime;

public class Memory {
    public enum Category {
        GENERAL, PREFERENCE, FACT, GOAL, EXPERIENCE, RELATIONSHIP
    }

    private int id;
    private String content;
    private Category category;
    private String source; // MANUAL, AI_SUGGESTED
    private boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Memory() {
        this.category = Category.GENERAL;
        this.source = "MANUAL";
        this.approved = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
