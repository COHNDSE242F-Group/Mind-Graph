package org.mindgraph.db;

import org.mindgraph.model.Note;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public class NoteDao {
    private final String url;

    public NoteDao(String dbPath) { this.url = "jdbc:sqlite:" + dbPath; }

    public void upsert(Note n) throws Exception {
        try (Connection c = DriverManager.getConnection(url)) {
            try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO notes(id,title,contentMarkup,created_at,updated_at)
                VALUES(?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE 
                  SET title=excluded.title, 
                      contentMarkup=excluded.contentMarkup, 
                      updated_at=excluded.updated_at;
            """)) {
                ps.setString(1, n.getId());
                ps.setString(2, n.getTitle());
                ps.setString(3, n.getContentMarkup());
                ps.setString(4, n.getCreatedAt().toString());
                ps.setString(5, LocalDateTime.now().toString());
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
                        return new Note(
                                rs.getString("id"),
                                rs.getString("title"),
                                rs.getString("contentMarkup"),
                                1, // difficulty default, or read from DB if available
                                LocalDateTime.parse(rs.getString("created_at")),
                                LocalDateTime.parse(rs.getString("updated_at"))
                        );
                    }
                    return null;
                }
            }
        }
    }
}