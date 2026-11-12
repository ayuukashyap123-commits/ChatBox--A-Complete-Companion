package com.chatbox.model;

import java.time.LocalDateTime;

public class Task {
    public enum Priority { LOW, MEDIUM, HIGH, URGENT }
    public enum Status { TODO, IN_PROGRESS, DONE, CANCELLED }

    private int id;
    private Integer goalId;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private String dueDate;
    private String completedAt;
    private LocalDateTime createdAt;

    public Task() {
        this.priority = Priority.MEDIUM;
        this.status = Status.TODO;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getGoalId() { return goalId; }
    public void setGoalId(Integer goalId) { this.goalId = goalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return title; }
}
