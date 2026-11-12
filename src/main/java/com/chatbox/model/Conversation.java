package com.chatbox.model;

import java.time.LocalDateTime;

public class Conversation {
    public enum Mode {
        GENERAL("General Assistant"),
        DOPPELGANGER("Digital Doppelgänger"),
        WELLNESS("Mental Wellness"),
        COACH("Life Coach"),
        KNOWLEDGE("Personal Knowledge");

        private final String label;
        Mode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private int id;
    private String title;
    private Mode mode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Conversation() {
        this.mode = Mode.GENERAL;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Conversation(String title, Mode mode) {
        this();
        this.title = title;
        this.mode = mode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return title; }
}
