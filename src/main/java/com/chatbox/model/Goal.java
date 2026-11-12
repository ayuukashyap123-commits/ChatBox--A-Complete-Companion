package com.chatbox.model;

import java.time.LocalDateTime;

public class Goal {
    public enum Status { ACTIVE, COMPLETED, PAUSED, ABANDONED }
    public enum Category { PERSONAL, CAREER, HEALTH, LEARNING, FINANCIAL, RELATIONSHIPS, OTHER }

    private int id;
    private String title;
    private String description;
    private Category category;
    private Status status;
    private String targetDate;
    private int progress; // 0-100
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Goal() {
        this.category = Category.PERSONAL;
        this.status = Status.ACTIVE;
        this.progress = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = Math.max(0, Math.min(100, progress)); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return title; }
}
