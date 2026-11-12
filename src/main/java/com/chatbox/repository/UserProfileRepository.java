package com.chatbox.repository;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.UserProfile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class UserProfileRepository {
    private static final Logger LOG = Logger.getLogger(UserProfileRepository.class.getName());

    public UserProfile getProfile() {
        String sql = "SELECT * FROM user_profile WHERE id = 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.warning("Error loading profile: " + e.getMessage());
        }
        return new UserProfile();
    }

    public void saveProfile(UserProfile profile) {
        String upsert = """
            INSERT INTO user_profile
                (id, name, age, occupation, interests, communication_style, personality, goals, preferred_name, bio, updated_at)
            VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))
            ON CONFLICT(id) DO UPDATE SET
                name=excluded.name, age=excluded.age, occupation=excluded.occupation,
                interests=excluded.interests, communication_style=excluded.communication_style,
                personality=excluded.personality, goals=excluded.goals,
                preferred_name=excluded.preferred_name, bio=excluded.bio,
                updated_at=datetime('now')
            """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setString(1, profile.getName());
            ps.setInt(2, profile.getAge());
            ps.setString(3, profile.getOccupation());
            ps.setString(4, profile.getInterests());
            ps.setString(5, profile.getCommunicationStyle());
            ps.setString(6, profile.getPersonality());
            ps.setString(7, profile.getGoals());
            ps.setString(8, profile.getPreferredName());
            ps.setString(9, profile.getBio());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Error saving profile: " + e.getMessage());
        }
    }

    private UserProfile mapRow(ResultSet rs) throws SQLException {
        UserProfile p = new UserProfile();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setAge(rs.getInt("age"));
        p.setOccupation(rs.getString("occupation"));
        p.setInterests(rs.getString("interests"));
        p.setCommunicationStyle(rs.getString("communication_style"));
        p.setPersonality(rs.getString("personality"));
        p.setGoals(rs.getString("goals"));
        p.setPreferredName(rs.getString("preferred_name"));
        p.setBio(rs.getString("bio"));
        String ts = rs.getString("updated_at");
        if (ts != null) p.setUpdatedAt(LocalDateTime.parse(ts.replace(" ", "T")));
        return p;
    }
}
