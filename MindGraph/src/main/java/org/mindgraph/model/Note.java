package org.mindgraph.model;

import java.time.LocalDateTime;

public class Note {
    private String id;
    private String title;
    private String contentMarkup; // Base64 encoded StyledDocument
    private int difficulty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Note() {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = "Untitled";
        this.contentMarkup = "";
        this.difficulty = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Note(String title, String contentMarkup) {
        this();
        this.title = title;
        this.contentMarkup = contentMarkup;
    }

    public Note(String id, String title, String contentMarkup, int difficulty,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.contentMarkup = contentMarkup;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}