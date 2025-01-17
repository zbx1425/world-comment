package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import io.lettuce.core.api.StatefulRedisConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;

public class RedisMessage {

    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";

    private static final long INSTANCE_ID = ServerWorldData.SNOWFLAKE.nextId();

    public long initiator;
    public Action action;
    public ByteBuf content;

    public RedisMessage(Action action, ByteBuf content) {
        this.initiator = INSTANCE_ID;
        this.action = action;
        this.content = content;
    }

    public RedisMessage(ByteBuf src) {
        this.action = Action.values()[src.readByte()];
        this.initiator = src.readLong();
        int length = src.readInt();
        this.content = src.readBytes(length);
    }

    public static RedisMessage insert(CommentEntry entry) {
        return new RedisMessage(Action.INSERT, entry.toBinaryBuffer());
    }

    public static RedisMessage update(CommentEntry entry) {
        return new RedisMessage(Action.UPDATE, entry.toBinaryBuffer());
    }

    public static RedisMessage updateAllFields(CommentEntry entry) {
        return new RedisMessage(Action.UPDATE_ALL_FIELDS, entry.toBinaryBuffer());
    }

    public void publishAsync(StatefulRedisConnection<String, ByteBuf> connection) {
        ByteBuf buffer = Unpooled.buffer(content.readableBytes() + 16);
        buffer.writeByte(action.ordinal());
        buffer.writeLong(initiator);
        buffer.writeInt(content.readableBytes());
        buffer.writeBytes(content);
        connection.async().publish(COMMAND_CHANNEL, buffer);
    }

    public void handle(RedisSynchronizer synchronizer) throws IOException {
        if (isFromSelf()) return;
        switch (action) {
            case INSERT:
                synchronizer.handleInsert(CommentEntry.fromBinaryBuffer(content));
                break;
            case UPDATE:
                synchronizer.handleUpdate(CommentEntry.fromBinaryBuffer(content));
                break;
            case UPDATE_ALL_FIELDS:
                synchronizer.handleUpdateAllFields(CommentEntry.fromBinaryBuffer(content));
                break;
        }
    }

    public boolean isFromSelf() {
        return initiator == INSTANCE_ID;
    }

    public enum Action {
        INSERT, UPDATE, UPDATE_ALL_FIELDS
    }
}
