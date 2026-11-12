package com.chatbox.model;

import java.time.LocalDateTime;

public class UserProfile {
    private int id = 1;
    private String name = "";
    private int age;
    private String occupation = "";
    private String interests = "";
    private String communicationStyle = "";
    private String personality = "";
    private String goals = "";
    private String preferredName = "";
    private String bio = "";
    private LocalDateTime updatedAt;

    public UserProfile() { this.updatedAt = LocalDateTime.now(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getCommunicationStyle() { return communicationStyle; }
    public void setCommunicationStyle(String communicationStyle) { this.communicationStyle = communicationStyle; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }
    public String getPreferredName() { return preferredName; }
    public void setPreferredName(String preferredName) { this.preferredName = preferredName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDisplayName() {
        if (preferredName != null && !preferredName.isBlank()) return preferredName;
        if (name != null && !name.isBlank()) return name;
        return "User";
    }
}
