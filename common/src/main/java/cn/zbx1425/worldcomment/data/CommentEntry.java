package cn.zbx1425.worldcomment.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CommentEntry {

    public long id;
    public long timestamp;
    public boolean deleted;
    public ResourceLocation level;
    public ChunkPos region;
    public BlockPos location;
    public UUID initiator;
    public String initiatorName;
    public int messageType;
    public String message;
    public String imageUrl;

    public CommentEntry(CommentTable table, ResultSet result) throws SQLException {
        int iota = 0;
        id = result.getLong(++iota);
        timestamp = result.getLong(++iota);
        deleted = result.getBoolean(++iota);
        level = table.db.dimensions.getDimensionById(result.getInt(++iota));
        region = new ChunkPos(result.getLong(++iota));
        location = new BlockPos(result.getInt(++iota), result.getInt(++iota), result.getInt(++iota));
        initiator = uuidFromByteArray(result.getBytes(++iota));
        initiatorName = result.getString(++iota);
        messageType = result.getInt(++iota);
        message = result.getString(++iota);
        imageUrl = result.getString(++iota);
    }

    private static UUID uuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public void insertTo(CommentTable table) throws SQLException {
        table.db.execute("""
            INSERT OR REPLACE INTO comments (
                id, timestamp, deleted, level, region, locationX, locationY, locationZ, initiator, initiatorName, messageType, message, imageUrl
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            );
            """, params -> {
            int iota = 0;
            if (id == 0) id = Database.SNOWFLAKE.nextId();
            params.setLong(++iota, id);
            params.setLong(++iota, timestamp);
            params.setBoolean(++iota, deleted);
            params.setInt(++iota, table.db.dimensions.getDimensionId(level));
            params.setLong(++iota, region.toLong());
            params.setInt(++iota, location.getX());
            params.setInt(++iota, location.getY());
            params.setInt(++iota, location.getZ());
            params.setBytes(++iota, UUIDUtil.uuidToByteArray(initiator));
            params.setString(++iota, initiatorName);
            params.setInt(++iota, messageType);
            params.setString(++iota, message);
            params.setString(++iota, imageUrl);
        });
    }

}
