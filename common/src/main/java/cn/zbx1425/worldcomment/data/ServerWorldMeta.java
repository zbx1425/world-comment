package cn.zbx1425.worldcomment.data;

import com.google.gson.JsonObject;

import java.util.UUID;

public class ServerWorldMeta {

    public UUID worldId;

    public ServerWorldMeta() {
        worldId = UUID.randomUUID();
    }

    public ServerWorldMeta(JsonObject serialized) {
        if (serialized.has("worldId")) {
            worldId = UUID.fromString(serialized.get("worldId").getAsString());
        } else {
            worldId = UUID.randomUUID();
        }
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("worldId", worldId.toString());
        return obj;
    }
}
