package org.mindgraph.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Note {
    private String id;
    private String title;
    private String contentMarkup; // Base64 encoded StyledDocument
    private int difficulty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // New field for keywords
    private List<String> keywords;

    // Constructors
    public Note() {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = "Untitled";
        this.contentMarkup = "";
        this.difficulty = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.keywords = new ArrayList<>();
    }

    public Note(String title, String contentMarkup) {
        this();
        this.title = title;
        this.contentMarkup = contentMarkup;
    }

    public Note(String id, String title, String contentMarkup, int difficulty,
                LocalDateTime createdAt, LocalDateTime updatedAt, List<String> keywords) {
        this.id = id;
        this.title = title;
        this.contentMarkup = contentMarkup;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.keywords = keywords != null ? keywords : new ArrayList<>();
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentMarkup() { return contentMarkup; }
    public void setContentMarkup(String contentMarkup) { this.contentMarkup = contentMarkup; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    // --- Helper methods for keywords ---
    public void addKeyword(String keyword) {
        if(!keywords.contains(keyword)) keywords.add(keyword);
    }

    public void removeKeyword(String keyword) {
        keywords.remove(keyword);
    }

    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword);
    }
}