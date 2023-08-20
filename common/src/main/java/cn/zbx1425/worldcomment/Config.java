package cn.zbx1425.worldcomment;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public String redisUrl = "";
    public boolean syncIsHost = true;

    public String uplinkUrl = "";
    public String uplinkAuthKey = "";

    public void load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) save(configPath);

        JsonObject json = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        redisUrl = json.get("redisUrl").getAsString();
        syncIsHost = json.get("syncIsHost").getAsBoolean();
        uplinkUrl = json.get("uplinkUrl").getAsString();
        uplinkAuthKey = json.get("uplinkAuthKey").getAsString();
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("redisUrl", redisUrl);
        json.addProperty("syncIsHost", syncIsHost);
        json.addProperty("uplinkUrl", uplinkUrl);
        json.addProperty("uplinkAuthKey", uplinkAuthKey);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json.toString()));
    }
}
