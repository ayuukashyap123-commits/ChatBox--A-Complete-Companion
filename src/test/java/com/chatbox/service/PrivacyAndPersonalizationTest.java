package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.*;
import com.chatbox.repository.ConversationRepository;
import com.chatbox.repository.MemoryRepository;
import com.chatbox.repository.WellnessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrivacyAndPersonalizationTest {

    private AiService aiService;
    private MemoryRepository memoryRepo;
    private WellnessRepository wellnessRepo;
    private ConversationRepository convRepo;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        DatabaseManager.getInstance().initialize();
        memoryRepo = new MemoryRepository();
        wellnessRepo = new WellnessRepository();
        convRepo = new ConversationRepository();
        aiService = new AiService();
        aiService.getConfig().setForceOffline(true);

        testProfile = new UserProfile();
        testProfile.setName("Jane Doe");
        testProfile.setPreferredName("Jane");
        testProfile.setInterests("Distributed Computing, Neuromorphic Hardware");
        testProfile.setCommunicationStyle("Direct, structured, and insightful");
        testProfile.setGoals("Publish research paper on edge AI systems");
    }

    @AfterEach
    void tearDown() {
        // Clean test memories
        for (Memory m : memoryRepo.findAll()) {
            if (m.getContent().startsWith("TestMemory_")) {
                memoryRepo.delete(m.getId());
            }
        }
        // Clean test journals
        for (JournalEntry j : wellnessRepo.findAllJournals()) {
            if (j.getTitle().startsWith("TestJournal_")) {
                wellnessRepo.deleteJournal(j.getId());
            }
        }
    }

    @Test
    void testApprovedMemoriesIncludedInSystemPrompt() {
        Memory approvedMemory = new Memory();
        approvedMemory.setContent("TestMemory_Prefers concise code without boilerplate.");
        approvedMemory.setCategory(Memory.Category.PREFERENCE);
        approvedMemory.setApproved(true);
        memoryRepo.save(approvedMemory);

        String systemPrompt = aiService.buildSystemPrompt(Conversation.Mode.GENERAL, testProfile);

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("TestMemory_Prefers concise code without boilerplate."));
        assertTrue(systemPrompt.contains("Approved User Memories"));
    }

    @Test
    void testUnapprovedMemoriesExcludedFromSystemPrompt() {
        Memory unapprovedMemory = new Memory();
        unapprovedMemory.setContent("TestMemory_ProposedCandidateNotYetReviewedByStudent");
        unapprovedMemory.setCategory(Memory.Category.FACT);
        unapprovedMemory.setApproved(false);
        memoryRepo.save(unapprovedMemory);

        String systemPrompt = aiService.buildSystemPrompt(Conversation.Mode.GENERAL, testProfile);

        assertNotNull(systemPrompt);
        assertFalse(systemPrompt.contains("TestMemory_ProposedCandidateNotYetReviewedByStudent"),
            "Unapproved or proposed memories must never be injected into AI prompts");
    }

    @Test
    void testJournalEntriesNeverLeakedToSystemPromptOrKnowledge() {
        JournalEntry privateJournal = new JournalEntry();
        privateJournal.setTitle("TestJournal_DeepSecretReflection");
        privateJournal.setContent("Confidential thoughts that should never be sent to external AI servers.");
        privateJournal.setMoodScore(3);
        wellnessRepo.saveJournal(privateJournal);

        for (Conversation.Mode mode : Conversation.Mode.values()) {
            String prompt = aiService.buildSystemPrompt(mode, testProfile);
            assertFalse(prompt.contains("DeepSecretReflection"),
                "Journal titles must never appear in AI system prompts for mode: " + mode);
            assertFalse(prompt.contains("Confidential thoughts"),
                "Journal content must never appear in AI system prompts for mode: " + mode);
        }
    }

    @Test
    void testChatMessagesNeverAutoSavedToMemories() {
        int initialMemoryCount = memoryRepo.findAll().size();

        Conversation conv = new Conversation("Test Privacy Conversation", Conversation.Mode.GENERAL);
        convRepo.save(conv);

        Message msg = new Message(conv.getId(), Message.Role.USER, "I want to share my private hobby: Competitive Origami");
        convRepo.saveMessage(msg);

        // Call chat
        aiService.chat(Conversation.Mode.GENERAL, msg.getContent(), List.of(msg), testProfile);

        int postChatMemoryCount = memoryRepo.findAll().size();
        assertEquals(initialMemoryCount, postChatMemoryCount,
            "Chat messages must never be automatically converted into permanent memories without explicit user action");

        // Clean up
        convRepo.delete(conv.getId());
    }

    @Test
    void testUserProfilePreferencesReflectedInPrompt() {
        String prompt = aiService.buildSystemPrompt(Conversation.Mode.GENERAL, testProfile);

        assertTrue(prompt.contains("Jane"));
        assertTrue(prompt.contains("Distributed Computing, Neuromorphic Hardware"));
        assertTrue(prompt.contains("Direct, structured, and insightful"));
        assertTrue(prompt.contains("Publish research paper on edge AI systems"));
    }
}
