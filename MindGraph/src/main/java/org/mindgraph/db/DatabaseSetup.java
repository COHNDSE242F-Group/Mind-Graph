package org.mindgraph.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseSetup {
    public static void init(String path) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + path)) {
            try (Statement s = c.createStatement()) {
                s.execute("""
          CREATE TABLE IF NOT EXISTS notes (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            content TEXT,
            created_at TEXT,
            updated_at TEXT
          );
        """);
            }
        }
    }
}
