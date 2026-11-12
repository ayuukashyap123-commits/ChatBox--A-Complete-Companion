package com.chatbox.service;

import com.chatbox.model.Memory;
import com.chatbox.repository.MemoryRepository;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service managing user-sovereign memory records:
 * - Explicit user control over approved vs pending memories.
 * - Only approved memories are injected into AI contextual prompts.
 * - Memories remain strictly isolated from private journal entries and raw chat history.
 */
public class MemoryService {
    private static final Logger LOG = Logger.getLogger(MemoryService.class.getName());

    private final MemoryRepository repo;

    public MemoryService() {
        this(new MemoryRepository());
    }

    public MemoryService(MemoryRepository repo) {
        this.repo = Objects.requireNonNull(repo, "MemoryRepository must not be null");
    }

    public List<Memory> getAllMemories() {
        return repo.findAll();
    }

    public List<Memory> getApprovedMemories() {
        return repo.findApproved();
    }

    public Memory getMemoryById(int id) {
        return repo.findById(id);
    }

    public Memory createMemory(String content, Memory.Category category, boolean approved) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Memory content cannot be empty.");
        }
        Memory m = new Memory();
        m.setContent(content.trim());
        m.setCategory(category != null ? category : Memory.Category.GENERAL);
        m.setSource("MANUAL");
        m.setApproved(approved);
        return repo.save(m);
    }

    public void updateMemory(Memory memory) {
        if (memory == null || memory.getContent() == null || memory.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid memory with content is required.");
        }
        memory.setContent(memory.getContent().trim());
        repo.update(memory);
    }

    public void toggleApproval(int memoryId) {
        Memory m = repo.findById(memoryId);
        if (m != null) {
            boolean nextState = !m.isApproved();
            repo.setApproved(memoryId, nextState);
        }
    }

    public void setApproval(int memoryId, boolean approved) {
        repo.setApproved(memoryId, approved);
    }

    public void deleteMemory(int memoryId) {
        repo.delete(memoryId);
    }
}
