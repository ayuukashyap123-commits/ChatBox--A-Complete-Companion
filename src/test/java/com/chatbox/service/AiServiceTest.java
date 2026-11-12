package com.chatbox.service;

import com.chatbox.model.Conversation;
import com.chatbox.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AiServiceTest {

    private AiService aiService;
    private UserProfile testProfile;
    private final com.chatbox.repository.SettingsRepository settingsRepo = new com.chatbox.repository.SettingsRepository();

    private String originalApiKey;
    private String originalModel;
    private String originalProvider;
    private String originalEndpoint;
    private String originalForceOffline;

    @BeforeEach
    void setUp() {
        backupSettings();
        aiService = new AiService();
        testProfile = new UserProfile();
        testProfile.setName("Alex Tester");
        testProfile.setPreferredName("Alex");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        restoreSettings();
    }

    private void backupSettings() {
        originalApiKey = settingsRepo.get("ai.api_key", null);
        originalModel = settingsRepo.get("ai.model", null);
        originalProvider = settingsRepo.get("ai.provider", null);
        originalEndpoint = settingsRepo.get("ai.endpoint", null);
        originalForceOffline = settingsRepo.get("ai.force_offline", null);

        settingsRepo.remove("ai.force_offline");
        settingsRepo.remove("ai.provider");
        settingsRepo.remove("ai.model");
        settingsRepo.remove("ai.endpoint");
        settingsRepo.remove("ai.api_key");
    }

    private void restoreSettings() {
        restoreSetting("ai.api_key", originalApiKey);
        restoreSetting("ai.model", originalModel);
        restoreSetting("ai.provider", originalProvider);
        restoreSetting("ai.endpoint", originalEndpoint);
        restoreSetting("ai.force_offline", originalForceOffline);
    }

    private void restoreSetting(String key, String value) {
        if (value != null) {
            settingsRepo.set(key, value);
        } else {
            settingsRepo.remove(key);
        }
    }

    @Test
    void testStatusLabelNotNull() {
        String label = aiService.getStatusLabel();
        assertNotNull(label);
        assertTrue(label.contains("Connected") || label.contains("Demo"));
    }

    @Test
    void testRuntimeApiKeyConfiguration() {
        aiService.setRuntimeApiKey("test-dummy-key-xyz");
        assertTrue(aiService.isLive());
        assertEquals("AI Connected", aiService.getStatusLabel());
        assertEquals("Runtime Key", aiService.getSourceInfo());

        // Reset back to offline
        aiService.setRuntimeApiKey(null);
        if (System.getenv("GEMINI_API_KEY") == null) {
            assertFalse(aiService.isLive());
            assertEquals("Local Demo", aiService.getStatusLabel());
        }
    }

    @Test
    void testDemoResponseGeneralMode() {
        if (!aiService.isLive()) {
            String resp = aiService.chat(Conversation.Mode.GENERAL, "Hello Chatbox", Collections.emptyList(), testProfile);
            assertNotNull(resp);
            assertTrue(resp.contains("Demo Mode"));
            assertTrue(resp.contains("Alex"));
            assertTrue(resp.contains("Hello Chatbox"));
        }
    }

    @Test
    void testDemoResponseWellnessMode() {
        if (!aiService.isLive()) {
            String resp = aiService.chat(Conversation.Mode.WELLNESS, "I feel stressed about deadlines", Collections.emptyList(), testProfile);
            assertNotNull(resp);
            assertTrue(resp.contains("Demo Mode"));
            assertTrue(resp.contains("Alex"));
            assertTrue(resp.contains("support") || resp.contains("reflect"));
        }
    }

    @Test
    void testDemoResponseCoachMode() {
        if (!aiService.isLive()) {
            String resp = aiService.chat(Conversation.Mode.COACH, "How do I build consistency?", Collections.emptyList(), testProfile);
            assertNotNull(resp);
            assertTrue(resp.contains("Demo Mode"));
            assertTrue(resp.contains("Alex"));
            assertTrue(resp.contains("milestones") || resp.contains("steps"));
        }
    }

    @Test
    void testDemoResponseDoppelgangerMode() {
        if (!aiService.isLive()) {
            String resp = aiService.chat(Conversation.Mode.DOPPELGANGER, "What are my priorities?", Collections.emptyList(), testProfile);
            assertNotNull(resp);
            assertTrue(resp.contains("Demo Mode"));
            assertTrue(resp.contains("Alex"));
        }
    }

    @Test
    void testDemoResponseKnowledgeMode() {
        if (!aiService.isLive()) {
            String resp = aiService.chat(Conversation.Mode.KNOWLEDGE, "What does my notes say?", Collections.emptyList(), testProfile);
            assertNotNull(resp);
            assertTrue(resp.contains("Demo Mode"));
            assertTrue(resp.contains("Second Brain"));
        }
    }
}
