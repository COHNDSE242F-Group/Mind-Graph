package org.mindgraph.db;

import org.mindgraph.model.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoteDao {

    private final String url;

    public NoteDao(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /**
     * Insert a new note or update if it exists.
     * Only metadata is stored in DB; actual content is in XML/HTML file.
     */
    public void upsert(Note note, String filePath) throws SQLException {
        if (note.getId() == 0) {
            insert(note, filePath);
        } else {
            update(note, filePath);
        }
    }

    private void insert(Note note, String filePath) throws SQLException {
        String sql = """
                INSERT INTO notes(title, file_path, keywords, difficulty, created_at, updated_at)
                VALUES(?,?,?,?,?,?)
                """;

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, filePath);
            ps.setString(3, note.keywordsAsCsv());
            ps.setInt(4, note.getDifficulty());
            ps.setString(5, note.getCreatedAt().toString());
            ps.setString(6, LocalDateTime.now().toString());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    note.setId(rs.getInt(1));
                }
            }
        }
    }

    private void update(Note note, String filePath) throws SQLException {
        String sql = """
                UPDATE notes SET
                    title = ?,
                    file_path = ?,
                    keywords = ?,
                    difficulty = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, filePath);
            ps.setString(3, note.keywordsAsCsv());
            ps.setInt(4, note.getDifficulty());
            ps.setString(5, LocalDateTime.now().toString());
            ps.setInt(6, note.getId());

            ps.executeUpdate();
        }
    }

    /** Load a note by ID (content is in file, not DB) */
    public Note load(int id) throws SQLException {
        String sql = "SELECT * FROM notes WHERE id = ?";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Note note = new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("difficulty"),
                        LocalDateTime.parse(rs.getString("created_at")),
                        LocalDateTime.parse(rs.getString("updated_at")),
                        Note.keywordsFromCsv(rs.getString("keywords"))
                );
                note.setFilePath(rs.getString("file_path"));
                return note;
            }
        }
    }

    /** Load a note by file path (needed for NotepadController) */
    public Note findByFilePath(String filePath) throws SQLException {
        String sql = "SELECT * FROM notes WHERE file_path = ?";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, filePath);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Note note = new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("difficulty"),
                        LocalDateTime.parse(rs.getString("created_at")),
                        LocalDateTime.parse(rs.getString("updated_at")),
                        Note.keywordsFromCsv(rs.getString("keywords"))
                );
                note.setFilePath(rs.getString("file_path"));
                return note;
            }
        }
    }

    /** Update only the keywords for a note */
    public void updateKeywords(int id, String keywordsCsv) throws SQLException {
        String sql = "UPDATE notes SET keywords = ?, updated_at = ? WHERE id = ?";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, keywordsCsv);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, id);

            ps.executeUpdate();
        }
    }

    public List<Note> findAll() throws SQLException {
        String sql = "SELECT * FROM notes";
        List<Note> notes = new ArrayList<>();

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Note note = new Note(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("difficulty"),
                        LocalDateTime.parse(rs.getString("created_at")),
                        LocalDateTime.parse(rs.getString("updated_at")),
                        Note.keywordsFromCsv(rs.getString("keywords"))
                );
                note.setFilePath(rs.getString("file_path"));
                notes.add(note);
            }
        }
        return notes;
    }
}