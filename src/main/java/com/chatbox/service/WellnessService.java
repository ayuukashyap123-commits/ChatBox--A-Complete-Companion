package com.chatbox.service;

import com.chatbox.model.JournalEntry;
import com.chatbox.model.MoodEntry;
import com.chatbox.repository.WellnessRepository;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service managing Mental Wellness functions:
 * - Real mood tracking, intensities, and historical trajectory summaries.
 * - Private reflective journal entries.
 * - Strict sovereignty guarantee: journal entries are never converted to memories.
 */
public class WellnessService {
    private static final Logger LOG = Logger.getLogger(WellnessService.class.getName());

    private final WellnessRepository repo;

    public WellnessService() {
        this(new WellnessRepository());
    }

    public WellnessService(WellnessRepository repo) {
        this.repo = Objects.requireNonNull(repo, "WellnessRepository must not be null");
    }

    // ─── Mood Check-Ins ───────────────────────────────────────────────────────

    public MoodEntry logMood(int score, String note) {
        int boundedScore = Math.max(1, Math.min(10, score));
        String label = MoodEntry.labelForScore(boundedScore);
        MoodEntry entry = new MoodEntry(boundedScore, label, note != null ? note.trim() : "");
        return repo.saveMood(entry);
    }

    public MoodEntry logMood(int score, String label, String note, String tags) {
        int boundedScore = Math.max(1, Math.min(10, score));
        String finalLabel = (label != null && !label.isBlank()) ? label.trim() : MoodEntry.labelForScore(boundedScore);
        MoodEntry entry = new MoodEntry(boundedScore, finalLabel, note != null ? note.trim() : "");
        if (tags != null && !tags.isBlank()) entry.setTags(tags.trim());
        return repo.saveMood(entry);
    }

    public List<MoodEntry> getRecentMoods(int limit) {
        return repo.findRecentMoods(Math.max(1, limit));
    }

    public List<MoodEntry> getAllMoods() {
        return repo.findAllMoods();
    }

    public MoodSummary calculateMoodSummary() {
        List<MoodEntry> all = repo.findAllMoods();
        if (all.isEmpty()) {
            return new MoodSummary(0, 0.0, 0, 0, "No records", null);
        }

        int total = all.size();
        double sum = 0.0;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        Map<String, Integer> freq = new HashMap<>();

        for (MoodEntry m : all) {
            int s = m.getMoodScore();
            sum += s;
            if (s > max) max = s;
            if (s < min) min = s;

            String lbl = m.getMoodLabel();
            if (lbl != null && !lbl.isBlank()) {
                freq.put(lbl, freq.getOrDefault(lbl, 0) + 1);
            }
        }

        double avg = Math.round((sum / total) * 10.0) / 10.0;
        String dominant = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Neutral");

        MoodEntry latest = all.get(0);
        return new MoodSummary(total, avg, max, min, dominant, latest);
    }

    // ─── Private Journaling ──────────────────────────────────────────────────

    public JournalEntry saveJournal(String title, String content, Integer moodScore, String tags) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Journal content cannot be empty.");
        }
        JournalEntry entry = new JournalEntry();
        entry.setTitle(title != null ? title.trim() : "");
        entry.setContent(content.trim());
        if (moodScore != null) {
            entry.setMoodScore(Math.max(1, Math.min(10, moodScore)));
        }
        if (tags != null) entry.setTags(tags.trim());
        return repo.saveJournal(entry);
    }

    public void updateJournal(JournalEntry entry) {
        if (entry == null || entry.getContent() == null || entry.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid journal entry with content is required.");
        }
        entry.setContent(entry.getContent().trim());
        if (entry.getTitle() != null) entry.setTitle(entry.getTitle().trim());
        repo.updateJournal(entry);
    }

    public void deleteJournal(int id) {
        repo.deleteJournal(id);
    }

    public List<JournalEntry> getAllJournals() {
        return repo.findAllJournals();
    }

    public JournalEntry getJournalById(int id) {
        return repo.findJournalById(id);
    }

    // ─── Mood Summary Data Record ────────────────────────────────────────────

    public record MoodSummary(
            int totalCheckIns,
            double averageScore,
            int highestScore,
            int lowestScore,
            String dominantMood,
            MoodEntry latestEntry
    ) {}
}
