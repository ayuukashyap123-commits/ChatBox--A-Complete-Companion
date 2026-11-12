package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.UserProfile;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProfileServiceTest {

    private static ProfileService service;

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
        service = new ProfileService();
    }

    @Test
    @Order(1)
    void testProfileRetrievalAndValidation() {
        UserProfile profile = service.getProfile();
        assertNotNull(profile);
        assertNotNull(profile.getDisplayName());

        String originalName = profile.getName();

        // Save with whitespace name -> defaults to "User"
        profile.setName("   ");
        service.saveProfile(profile);
        assertEquals("User", service.getProfile().getName());

        // Save valid profile
        profile.setName("Ayush Test");
        profile.setPreferredName("Ayush");
        profile.setAge(22);
        profile.setOccupation("Software Engineer");
        service.saveProfile(profile);

        UserProfile reloaded = service.getProfile();
        assertEquals("Ayush Test", reloaded.getName());
        assertEquals("Ayush", reloaded.getPreferredName());
        assertEquals(22, reloaded.getAge());

        // Restore
        profile.setName(originalName);
        service.saveProfile(profile);
    }
}
