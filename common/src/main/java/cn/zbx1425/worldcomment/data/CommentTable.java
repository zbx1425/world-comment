package cn.zbx1425.worldcomment.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommentTable {

    public final Database db;
    public int maxId;

    public CommentTable(Database db) {
        this.db = db;
    }

    public void init() throws SQLException {
        db.execute("""
            CREATE TABLE IF NOT EXISTS comments (
                id            INTEGER PRIMARY KEY,
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

    public List<CommentEntry> queryInRegion(ResourceLocation level, ChunkPos region) throws SQLException {
        ArrayList<CommentEntry> entries = new ArrayList<>();
        try (ResultSet result = db.executeQuery(
            "SELECT * FROM comments WHERE level = ? AND region = ?", params -> {
                params.setInt(1, db.dimensions.getDimensionId(level));
                params.setLong(2, region.toLong());
            }
        )) {
            while (result.next()) {
                entries.add(new CommentEntry(this, result));
            }
        }
        return entries;
    }

    public List<CommentEntry> queryInTime(long from) throws SQLException {
        ArrayList<CommentEntry> entries = new ArrayList<>();
        try (ResultSet result = db.executeQuery(
                "SELECT * FROM comments WHERE timestamp > ?", params -> {
                    params.setLong(1, from);
                }
        )) {
            while (result.next()) {
                entries.add(new CommentEntry(this, result));
            }
        }
        return entries;
    }
}
