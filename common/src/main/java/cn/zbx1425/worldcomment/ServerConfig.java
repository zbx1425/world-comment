package cn.zbx1425.worldcomment;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServerConfig {

    public ConfigItem redisUrl;
    public ConfigItem syncRole;

    public ConfigItem uplinkUrl;
    public ConfigItem uplinkAuthKey;

    public ConfigItem imageUploadConfig;

    public ConfigItem allowMarkerUsage;
    public ConfigItem commentVisibilityCriteria;
    public ConfigItem markerVisibilityCriteria;

    public void load(Path configPath) throws IOException {
        JsonObject json = Files.exists(configPath)
                ? JsonParser.parseString(Files.readString(configPath)).getAsJsonObject()
                : new JsonObject();
        redisUrl = new ConfigItem(json, "redisUrl", "");
        syncRole = new ConfigItem(json, "syncRole", "host");
        uplinkUrl = new ConfigItem(json, "uplinkUrl", "");
        uplinkAuthKey = new ConfigItem(json, "uplinkAuthKey", "");
        imageUploadConfig = new ConfigItem(json, "imageUploadConfig", "");
        allowMarkerUsage = new ConfigItem(json, "allowMarkerUsage", "creative");
        commentVisibilityCriteria = new ConfigItem(json, "commentVisibilityCriteria", "tool_toggle");
        markerVisibilityCriteria = new ConfigItem(json, "markerVisibilityCriteria", "always");

        if (!Files.exists(configPath)) save(configPath);
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();
        redisUrl.writeJson(json);
        syncRole.writeJson(json);
        uplinkUrl.writeJson(json);
        uplinkAuthKey.writeJson(json);
        imageUploadConfig.writeJson(json);
        allowMarkerUsage.writeJson(json);
        commentVisibilityCriteria.writeJson(json);
        markerVisibilityCriteria.writeJson(json);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public List<JsonObject> parseUploaderConfig() {
        List<JsonObject> uploaderConfigs = new ArrayList<>();
        try {
            JsonElement rootElement = JsonParser.parseString(imageUploadConfig.value);
            if (rootElement.isJsonArray()) {
                for (JsonElement element : rootElement.getAsJsonArray()) {
                    uploaderConfigs.add(element.getAsJsonObject());
                }
            } else if (rootElement.isJsonObject()) {
                uploaderConfigs.add(rootElement.getAsJsonObject());
            }
        } catch (Exception ex) {
            Main.LOGGER.error("Failed to parse image upload config", ex);
        }
        return uploaderConfigs;
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
                if (jsonObject.get(camelKey).isJsonArray() || jsonObject.get(camelKey).isJsonObject()) {
                    this.value = jsonObject.get(camelKey).toString();
                } else {
                    this.value = jsonObject.get(camelKey).getAsString();
                }
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
