package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisMessage {

    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";

    private static final String INSTANCE_ID = Long.toHexString(ServerWorldData.SNOWFLAKE.nextId());

    public String initiator;
    public String action;
    public String content;

    public RedisMessage(String action, String content) {
        this.initiator = INSTANCE_ID;
        this.action = action;
        this.content = content;
    }

    public RedisMessage(String redisCommand) {
        int firstHash = redisCommand.indexOf(':');
        int lastHash = redisCommand.lastIndexOf(':');
        this.action = redisCommand.substring(0, firstHash);
        this.initiator = redisCommand.substring(firstHash + 1, lastHash);
        this.content = redisCommand.substring(lastHash + 1);
    }

    public static RedisMessage insert(CommentEntry entry) {
        return new RedisMessage("INSERT", entry.toBinaryString());
    }

    public static RedisMessage update(CommentEntry entry) {
        return new RedisMessage("UPDATE", entry.toBinaryString());
    }

    public void publishAsync(StatefulRedisConnection<String, String> connection) {
        connection.async().publish(COMMAND_CHANNEL, String.format("%s:%s:%s", action, initiator, content));
    }

    public boolean isFromSelf() {
        return initiator.equals(INSTANCE_ID);
    }
}
