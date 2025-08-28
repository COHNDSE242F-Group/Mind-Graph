package org.mindgraph.db;

import org.mindgraph.model.Note;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class NoteDao {
    private final String url;

    public NoteDao(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    public void upsert(Note n) throws Exception {
        try (Connection c = DriverManager.getConnection(url)) {
            try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO notes(id,title,contentMarkup,keywords,created_at,updated_at)
                VALUES(?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE 
                  SET title=excluded.title, 
                      contentMarkup=excluded.contentMarkup,
                      keywords=excluded.keywords,
                      updated_at=excluded.updated_at;
            """)) {
                ps.setString(1, n.getId());
                ps.setString(2, n.getTitle());
                ps.setString(3, n.getContentMarkup());
                ps.setString(4, String.join(",", n.getKeywords())); // store as CSV
                ps.setString(5, n.getCreatedAt().toString());
                ps.setString(6, LocalDateTime.now().toString());
                ps.executeUpdate();
            }
        }
    }

    public Note load(String id) throws Exception {
        try (Connection c = DriverManager.getConnection(url)) {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM notes WHERE id=?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Note note = new Note(
                                rs.getString("id"),
                                rs.getString("title"),
                                rs.getString("contentMarkup"),
                                1, // difficulty default (or add column later)
                                LocalDateTime.parse(rs.getString("created_at")),
                                LocalDateTime.parse(rs.getString("updated_at"))
                        );
                        // Restore keywords list from CSV
                        String keywordsCsv = rs.getString("keywords");
                        if (keywordsCsv != null && !keywordsCsv.isBlank()) {
                            List<String> keywords = Arrays.asList(keywordsCsv.split(","));
                            note.setKeywords(keywords);
                        }
                        return note;
                    }
                    return null;
                }
            }
        }
    }
}