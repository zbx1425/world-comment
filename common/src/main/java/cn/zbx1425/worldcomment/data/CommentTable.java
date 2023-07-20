package cn.zbx1425.worldcomment.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CommentTable {

    public final Database db;
    public int maxId;

    public CommentTable(Database db) {
        this.db = db;
    }

    public void init() throws SQLException {
        db.execute("""
            CREATE TABLE IF NOT EXISTS comments (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp     INTEGER,
                deleted       INTEGER,
                level         INTEGER,
                region        INTEGER,
                locationX     INTEGER,
                locationY     INTEGER,
                locationZ     INTEGER,
                initiator     BLOB,
                initiatorName TEXT,
                messageType   INTEGER,
                message       TEXT,
                imageUrl      TEXT
            );
            CREATE INDEX IF NOT EXISTS regionIndex ON comments (
                level,
                region
            );
            CREATE INDEX IF NOT EXISTS timestampIndex ON comments (
                timestamp
            );
            """);
    }

    public void fetchMaxId() throws SQLException {
        try (ResultSet result = db.executeQuery("SELECT id FROM comments ORDER BY id DESC LIMIT 1;")) {
            if (!result.next()) {
                maxId = 0;
            } else {
                maxId = result.getInt(1);
            }
        }
    }
}
