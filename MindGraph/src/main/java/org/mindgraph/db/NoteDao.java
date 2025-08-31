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
    public void createSessionTable() throws Exception {
        String sql = """
        CREATE TABLE IF NOT EXISTS SessionHistory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        note_id TEXT NOT NULL,
                        file_path TEXT NOT NULL,
                        opened_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(note_id) REFERENCES notes(id) ON DELETE CASCADE
                      );
                
    """;

        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }


    public void saveSession(Note note) {
        if(note.getId() == 0 || note.getFilePath() == null) return; // skip invalid notes

        String sql = "INSERT INTO SessionHistory(note_id, file_path) VALUES(?, ?)";

        try (var conn = DriverManager.getConnection(url);
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, String.valueOf(note.getId()));  // note_id as TEXT
            pstmt.setString(2, note.getFilePath());            // file_path
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); // log errors
        }
    }

    public void updateSession(Note note) {
        String sql = "UPDATE SessionHistory SET opened_at = CURRENT_TIMESTAMP WHERE note_id = ?";

        try (var conn = DriverManager.getConnection(url);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(note.getId()));
            int rows = pstmt.executeUpdate();
            if(rows == 0){
                // If note not in session, insert new
                saveSession(note);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }




    public List<Note> getSessionHistory(String sortMode) throws Exception {
        List<Note> history = new ArrayList<>();
        String orderBy;

        switch(sortMode.toLowerCase()) {
            case "oldest": orderBy = "s.opened_at ASC"; break;
            case "mostused": orderBy = "s.usage_count DESC"; break; // you need a usage_count column
            default: orderBy = "s.opened_at DESC"; // newest first
        }

        String sql = """
    SELECT n.id, n.title, n.difficulty, n.created_at, n.updated_at, n.keywords, n.file_path
    FROM SessionHistory s
    JOIN notes n ON s.note_id = n.id
    ORDER BY """ + " " + orderBy;


        try (var conn = DriverManager.getConnection(url);
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

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
                history.add(note);
            }
        }

        return history;
    }

    public void incrementUsageCount(int noteId) throws SQLException {
        String sql = "UPDATE SessionHistory SET usage_count = usage_count + 1 WHERE id = ?";

        try (var conn = DriverManager.getConnection(url);
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            int rows = pstmt.executeUpdate();
            if(rows == 0){


                // If note not in session, insert new
               // saveSession(findById(noteId));
            }
        }
    }








}