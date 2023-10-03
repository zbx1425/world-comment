package cn.zbx1425.worldcomment;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {

    public ConfigItem redisUrl;
    public ConfigItem syncRole;

    public ConfigItem uplinkUrl;
    public ConfigItem uplinkAuthKey;

    public ConfigItem imageUploadConfig;

    public void load(Path configPath) throws IOException {
        JsonObject json = Files.exists(configPath)
                ? JsonParser.parseString(Files.readString(configPath)).getAsJsonObject()
                : new JsonObject();
        redisUrl = new ConfigItem(json, "redisUrl", "");
        syncRole = new ConfigItem(json, "syncRole", "host");
        uplinkUrl = new ConfigItem(json, "uplinkUrl", "");
        uplinkAuthKey = new ConfigItem(json, "uplinkAuthKey", "");
        imageUploadConfig = new ConfigItem(json, "imageUploadConfig", "");

        if (!Files.exists(configPath)) save(configPath);
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();
        redisUrl.writeJson(json);
        syncRole.writeJson(json);
        uplinkUrl.writeJson(json);
        uplinkAuthKey.writeJson(json);
        imageUploadConfig.writeJson(json);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public static class ConfigItem {

        private final String camelKey;
        public String value;
        public boolean isFromJson;

        public ConfigItem(JsonObject jsonObject, String camelKey, String defaultValue) {
            String snakeKey = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelKey);
            this.camelKey = camelKey;
            if (System.getenv("SUBNOTEICA_" + snakeKey) != null) {
                this.value = System.getenv("SUBNOTEICA_" + snakeKey);
                this.isFromJson = false;
            } else if (jsonObject.has(camelKey)) {
                this.value = jsonObject.get(camelKey).getAsString();
                this.isFromJson = true;
            } else {
                this.value = defaultValue;
                this.isFromJson = false;
            }
        }

        public void writeJson(JsonObject jsonObject) {
            if (isFromJson) {
                jsonObject.addProperty(camelKey, value);
            }
        }
    }
}
