package com.chatbox.service;

import com.chatbox.model.UserProfile;
import com.chatbox.repository.UserProfileRepository;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service managing user identity and persona for the Digital Doppelgänger:
 * - Profile validation and persistence.
 * - System prompt synthesis for behavioral modeling.
 * - Allows full edits without requiring an AI API key.
 */
public class ProfileService {
    private static final Logger LOG = Logger.getLogger(ProfileService.class.getName());

    private final UserProfileRepository repo;

    public ProfileService() {
        this(new UserProfileRepository());
    }

    public ProfileService(UserProfileRepository repo) {
        this.repo = Objects.requireNonNull(repo, "UserProfileRepository must not be null");
    }

    public UserProfile getProfile() {
        UserProfile p = repo.getProfile();
        return p != null ? p : new UserProfile();
    }

    public UserProfile saveProfile(String name, String preferredName, int age, String occupation,
                                   String bio, String interests, String personality,
                                   String communicationStyle, String goals) {
        UserProfile profile = new UserProfile();
        profile.setId(1);

        String trimmedName = (name != null && !name.trim().isEmpty()) ? name.trim() : "User";
        profile.setName(trimmedName);
        profile.setPreferredName(preferredName != null ? preferredName.trim() : "");
        profile.setAge(Math.max(0, Math.min(120, age)));
        profile.setOccupation(occupation != null ? occupation.trim() : "");
        profile.setBio(bio != null ? bio.trim() : "");
        profile.setInterests(interests != null ? interests.trim() : "");
        profile.setPersonality(personality != null ? personality.trim() : "");
        profile.setCommunicationStyle(communicationStyle != null ? communicationStyle.trim() : "");
        profile.setGoals(goals != null ? goals.trim() : "");

        repo.saveProfile(profile);
        return profile;
    }

    public void saveProfile(UserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        if (profile.getName() == null || profile.getName().trim().isEmpty()) {
            profile.setName("User");
        } else {
            profile.setName(profile.getName().trim());
        }
        if (profile.getPreferredName() != null) profile.setPreferredName(profile.getPreferredName().trim());
        profile.setAge(Math.max(0, Math.min(120, profile.getAge())));
        if (profile.getOccupation() != null) profile.setOccupation(profile.getOccupation().trim());
        if (profile.getBio() != null) profile.setBio(profile.getBio().trim());
        if (profile.getInterests() != null) profile.setInterests(profile.getInterests().trim());
        if (profile.getPersonality() != null) profile.setPersonality(profile.getPersonality().trim());
        if (profile.getCommunicationStyle() != null) profile.setCommunicationStyle(profile.getCommunicationStyle().trim());
        if (profile.getGoals() != null) profile.setGoals(profile.getGoals().trim());

        repo.saveProfile(profile);
    }
}
