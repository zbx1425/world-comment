package cn.zbx1425.worldcomment;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServerConfig {

    public enum SyncRole { HOST, SUBORDINATE }
    public enum MarkerUsage { OP, CREATIVE, ALL }
    public enum Visibility { NEVER, ALWAYS, PREFERENCE }

    public static class ConfigItem<T> {

        private final String camelKey;
        public final T value;
        public final boolean isPresentInJson;
        private final JsonElement jsonRawValue;

        public ConfigItem(JsonObject json, String camelKey, Supplier<T> defaultValue, Function<String, T> stringParser) {
            T toBeValue;
            this.camelKey = camelKey;

            String snakeKey = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelKey);
            String envValueStr = System.getenv("SUBNOTEICA_" + snakeKey);

            if (json.has(camelKey)) {
                this.isPresentInJson = true;
                this.jsonRawValue = json.get(camelKey);
            } else {
                this.isPresentInJson = false;
                this.jsonRawValue = null;
            }

            if (envValueStr != null) {
                toBeValue = stringParser.apply(envValueStr);
            } else {
                if (isPresentInJson) {
                    try {
                        if (jsonRawValue.isJsonPrimitive()) {
                            toBeValue = stringParser.apply(jsonRawValue.getAsString());
                        } else {
                            toBeValue = stringParser.apply(jsonRawValue.toString());
                        }
                    } catch (Exception e) {
                        Main.LOGGER.warn("Failed to parse JSON value for " + camelKey, e);
                        toBeValue = defaultValue.get();
                    }
                } else {
                    toBeValue = defaultValue.get();
                }
            }
            this.value = toBeValue;
        }

        public ConfigItem(JsonObject json, String camelKey, T defaultValue, Function<String, T> stringParser) {
            this(json, camelKey, () -> defaultValue, stringParser);
        }

        private ConfigItem(String camelKey, T value, boolean isPresentInJson, JsonElement jsonRawValue) {
            this.camelKey = camelKey;
            this.value = value;
            this.isPresentInJson = isPresentInJson;
            this.jsonRawValue = jsonRawValue;
        }

        public ConfigItem<T> withNewValueToPersist(T newValue, JsonElement jsonRawValue) {
            return new ConfigItem<>(camelKey, newValue, true, jsonRawValue);
        }

        public void writeJson(JsonObject json) {
            if (isPresentInJson) {
                json.add(camelKey, jsonRawValue);
            }
        }
    }

    public ConfigItem<SyncRole> syncRole;
    public ConfigItem<String> redisUrl;
    public ConfigItem<String> uplinkUrl;
    public ConfigItem<String> uplinkAuthKey;
    public ConfigItem<List<ImageUploader>> imageUploaders;
    public ConfigItem<MarkerUsage> allowMarkerUsage;
    public ConfigItem<Visibility> commentVisibilityCriteria;
    public ConfigItem<Visibility> markerVisibilityCriteria;
    public ConfigItem<Boolean> imageGlobalKill;
    public ConfigItem<Boolean> screenshotKeyTriggersComment;
    public ConfigItem<Boolean> defaultCommentVisibilityPreference;

    private Path path;

    private static <T extends Enum<T>> T parseEnum(String name, Class<T> enumClass) {
        return Enum.valueOf(enumClass, name.toUpperCase());
    }

    public void load(Path configPath) throws IOException {
        this.path = configPath;
        JsonObject json = Files.exists(configPath)
                ? JsonParser.parseString(Files.readString(configPath)).getAsJsonObject()
                : new JsonObject();
        
        redisUrl = new ConfigItem<>(json, "redisUrl", "", value -> value);
        syncRole = new ConfigItem<>(json, "syncRole", SyncRole.HOST, str -> parseEnum(str, SyncRole.class));
        uplinkUrl = new ConfigItem<>(json, "uplinkUrl", "", value -> value);
        uplinkAuthKey = new ConfigItem<>(json, "uplinkAuthKey", "", value -> value);
        imageUploaders = new ConfigItem<List<ImageUploader>>(json, "imageUploadConfig", () -> ImageUploader.parseUploaderList(List.of()), str -> {
            List<JsonObject> uploaderConfigs = new ArrayList<>();
            try {
                JsonElement rootElement = JsonParser.parseString(str);
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
            return ImageUploader.parseUploaderList(uploaderConfigs);
        });
        allowMarkerUsage = new ConfigItem<>(json, "allowMarkerUsage", MarkerUsage.CREATIVE, str -> parseEnum(str, MarkerUsage.class));
        commentVisibilityCriteria = new ConfigItem<>(json, "commentVisibilityCriteria", Visibility.PREFERENCE, str -> parseEnum(str, Visibility.class));
        markerVisibilityCriteria = new ConfigItem<>(json, "markerVisibilityCriteria", Visibility.ALWAYS, str -> parseEnum(str, Visibility.class));
        imageGlobalKill = new ConfigItem<>(json, "imageGlobalKill", false, Boolean::parseBoolean);
        screenshotKeyTriggersComment = new ConfigItem<>(json, "screenshotKeyTriggersComment", true, Boolean::parseBoolean);
        defaultCommentVisibilityPreference = new ConfigItem<>(json, "defaultCommentVisibilityPreference", false, Boolean::parseBoolean);

        if (!Files.exists(configPath)) save(configPath);
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();
        redisUrl.writeJson(json);
        syncRole.writeJson(json);
        uplinkUrl.writeJson(json);
        uplinkAuthKey.writeJson(json);
        imageUploaders.writeJson(json);
        allowMarkerUsage.writeJson(json);
        commentVisibilityCriteria.writeJson(json);
        markerVisibilityCriteria.writeJson(json);
        imageGlobalKill.writeJson(json);
        screenshotKeyTriggersComment.writeJson(json);
        defaultCommentVisibilityPreference.writeJson(json);
        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public void save() throws IOException {
        save(this.path);
    }
}
