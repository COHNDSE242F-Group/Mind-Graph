package org.mindgraph.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** Creates table if missing and performs gentle migrations if columns are missing. */
public class DatabaseSetup {
    public static void init(String path) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            try (Statement s = c.createStatement()) {
                // Base table
                s.execute("""
                    CREATE TABLE IF NOT EXISTS notes (
                      id TEXT PRIMARY KEY,
                      title TEXT NOT NULL,
                      file_path TEXT,        -- may be null initially; we'll backfill on first save
                      keywords TEXT,         -- CSV
                      difficulty INTEGER DEFAULT 1,
                      created_at TEXT,
                      updated_at TEXT
                    );
                """);


                // Create session history table
                s.execute("""
                   CREATE TABLE IF NOT EXISTS SessionHistory (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                note_id TEXT NOT NULL,
                                file_path TEXT NOT NULL,
                                opened_at TEXT DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY(note_id) REFERENCES notes(id) ON DELETE CASCADE
                              );
                        
                """);


                // Migrations for older schemas
                try { s.execute("ALTER TABLE notes ADD COLUMN file_path TEXT;"); }
                catch (Exception ignore) { /* duplicate column name */ }

                try { s.execute("ALTER TABLE notes ADD COLUMN keywords TEXT;"); }
                catch (Exception ignore) { /* duplicate column name */ }

                try { s.execute("ALTER TABLE notes ADD COLUMN difficulty INTEGER DEFAULT 1;"); }
                catch (Exception ignore) { /* duplicate column name */ }

                try { s.execute("ALTER TABLE notes ADD COLUMN created_at TEXT;"); }
                catch (Exception ignore) { /* duplicate column name */ }

                try { s.execute("ALTER TABLE notes ADD COLUMN updated_at TEXT;"); }
                catch (Exception ignore) { /* duplicate column name */ }
            }
        }
    }
}