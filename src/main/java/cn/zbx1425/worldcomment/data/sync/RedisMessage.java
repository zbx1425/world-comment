package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.lettuce.core.api.StatefulRedisConnection;

import java.security.SecureRandom;

public class RedisMessage {

    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";

    private static final long INSTANCE_ID = new SecureRandom().nextLong();

    public long initiator;
    public Action action;
    public long entryId;

    public RedisMessage(Action action, long entryId) {
        this.initiator = INSTANCE_ID;
        this.action = action;
        this.entryId = entryId;
    }

    public RedisMessage(String src) {
        JsonObject json = JsonParser.parseString(src).getAsJsonObject();
        this.action = Action.valueOf(json.get("action").getAsString());
        this.initiator = json.get("initiator").getAsLong();
        this.entryId = json.get("entryId").getAsLong();
    }

    public String serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("action", action.name());
        json.addProperty("initiator", initiator);
        json.addProperty("entryId", entryId);
        return json.toString();
    }

    public void publishAsync(StatefulRedisConnection<String, String> connection) {
        connection.async().publish(COMMAND_CHANNEL, serialize()).whenComplete((receivers, ex) -> {
            if (ex != null) Main.LOGGER.error("Failed to publish sync notification", ex);
        });
    }

    public void handle(RedisSynchronizer synchronizer) {
        if (isFromSelf()) return;
        synchronizer.handleNotification(action, entryId);
    }

    public boolean isFromSelf() {
        return initiator == INSTANCE_ID;
    }

    public enum Action {
        INSERT, UPDATE, UPDATE_ALL_FIELDS
    }
}
