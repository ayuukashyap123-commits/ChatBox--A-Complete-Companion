package com.chatbox.model;

import java.time.LocalDateTime;

public class Message {
    public enum Role { USER, ASSISTANT, SYSTEM }

    private int id;
    private int conversationId;
    private Role role;
    private String content;
    private LocalDateTime createdAt;

    public Message() { this.createdAt = LocalDateTime.now(); }

    public Message(int conversationId, Role role, String content) {
        this();
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
