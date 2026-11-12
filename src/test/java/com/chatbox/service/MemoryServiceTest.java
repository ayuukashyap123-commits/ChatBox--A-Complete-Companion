package com.chatbox.service;

import com.chatbox.database.DatabaseManager;
import com.chatbox.model.Memory;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemoryServiceTest {

    private static MemoryService service;

    @BeforeAll
    static void setup() {
        DatabaseManager.getInstance().initialize();
        service = new MemoryService();
    }

    @Test
    @Order(1)
    void testMemoryLifecycleAndApprovalToggle() {
        assertThrows(IllegalArgumentException.class, () -> service.createMemory("  ", Memory.Category.GENERAL, true));

        Memory memory = service.createMemory("Prefers dark theme IDEs and terminal workflows.", Memory.Category.PREFERENCE, true);
        assertNotNull(memory);
        assertTrue(memory.getId() > 0);
        assertTrue(memory.isApproved());

        List<Memory> approved = service.getApprovedMemories();
        assertTrue(approved.stream().anyMatch(m -> m.getId() == memory.getId()));

        // Toggle approval off
        service.toggleApproval(memory.getId());
        approved = service.getApprovedMemories();
        assertFalse(approved.stream().anyMatch(m -> m.getId() == memory.getId()), "Unapproved memory must not be in approved list");

        // Toggle back on
        service.toggleApproval(memory.getId());
        approved = service.getApprovedMemories();
        assertTrue(approved.stream().anyMatch(m -> m.getId() == memory.getId()));

        // Clean up
        service.deleteMemory(memory.getId());
        assertNull(service.getMemoryById(memory.getId()));
    }
}
