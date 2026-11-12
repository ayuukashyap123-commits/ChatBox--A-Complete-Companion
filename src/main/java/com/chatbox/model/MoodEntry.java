package com.chatbox.model;

import java.time.LocalDateTime;

public class MoodEntry {
    private int id;
    private int moodScore; // 1-10
    private String moodLabel;
    private String note;
    private String tags;
    private LocalDateTime createdAt;

    public MoodEntry() { this.createdAt = LocalDateTime.now(); }

    public MoodEntry(int moodScore, String moodLabel, String note) {
        this();
        this.moodScore = moodScore;
        this.moodLabel = moodLabel;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMoodScore() { return moodScore; }
    public void setMoodScore(int moodScore) { this.moodScore = moodScore; }
    public String getMoodLabel() { return moodLabel; }
    public void setMoodLabel(String moodLabel) { this.moodLabel = moodLabel; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static String labelForScore(int score) {
        return switch (score) {
            case 1, 2 -> "Struggling";
            case 3, 4 -> "Low";
            case 5, 6 -> "Neutral";
            case 7, 8 -> "Good";
            case 9, 10 -> "Excellent";
            default -> "Unknown";
        };
    }
}
