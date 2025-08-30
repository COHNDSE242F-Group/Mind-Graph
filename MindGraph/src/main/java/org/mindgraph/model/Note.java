package org.mindgraph.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Note implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int id; // 0 means "not yet persisted in DB"
    private String title;
    private int difficulty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> keywords;
    private String filePath; // path to XML/HTML content

    // Default constructor for a new note
    public Note() {
        this.id = 0;
        this.title = "Untitled";
        this.difficulty = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.keywords = new ArrayList<>();
        this.filePath = "";
    }

    // Constructor for title only
    public Note(String title) {
        this();
        this.title = title;
    }

    // Full constructor (e.g., loading from DB)
    public Note(int id, String title, int difficulty, LocalDateTime createdAt,
                LocalDateTime updatedAt, List<String> keywords) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.keywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.filePath = "";
    }

    // --- CSV helpers ---
    public String keywordsAsCsv() {
        return String.join(",", keywords);
    }

    public static List<String> keywordsFromCsv(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split("\\s*,\\s*"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    /** Returns keywords as clickable HTML links */
    public String keywordsAsLinksHtml() {
        if (keywords.isEmpty()) return "";
        return keywords.stream()
                .map(k -> "<a href='#' class='keyword-link'>" + k + "</a>")
                .collect(Collectors.joining(", "));
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getKeywords() { return List.copyOf(keywords); }
    public void setKeywords(List<String> keywords) { this.keywords = new ArrayList<>(keywords); }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    // --- Keyword management helpers ---
    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isBlank() && !keywords.contains(keyword)) {
            keywords.add(keyword);
        }
    }

    public void removeKeyword(String keyword) {
        keywords.remove(keyword);
    }

    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Note other = (Note) obj;
        return id == other.id; // equality based only on id
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}