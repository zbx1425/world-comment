package cn.zbx1425.worldcomment;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.ImageVariantConfig;

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

        /**
         * @param fileJson      raw JSON from the config file; only determines whether this
         *                      item gets persisted on save
         * @param effectiveJson file JSON already overlaid with environment variables via
         *                      {@link ConfigEnvironment}; the value is resolved from here
         */
        public ConfigItem(JsonObject fileJson, JsonObject effectiveJson, String camelKey, Supplier<T> defaultValue, Function<JsonElement, T> parser) {
            T toBeValue;
            this.camelKey = camelKey;

            if (fileJson.has(camelKey)) {
                this.isPresentInJson = true;
                this.jsonRawValue = fileJson.get(camelKey);
            } else {
                this.isPresentInJson = false;
                this.jsonRawValue = null;
            }

            if (effectiveJson.has(camelKey)) {
                try {
                    toBeValue = parser.apply(effectiveJson.get(camelKey));
                } catch (Exception e) {
                    Main.LOGGER.warn("Failed to parse JSON value for " + camelKey, e);
                    toBeValue = defaultValue.get();
                }
            } else {
                toBeValue = defaultValue.get();
            }
            this.value = toBeValue;
        }

        public ConfigItem(JsonObject fileJson, JsonObject effectiveJson, String camelKey, T defaultValue, Function<JsonElement, T> parser) {
            this(fileJson, effectiveJson, camelKey, () -> defaultValue, parser);
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
    public ConfigItem<Integer> syncNodeId;
    public ConfigItem<String> uplinkUrl;
    public ConfigItem<String> uplinkAuthKey;
    public ConfigItem<ImageVariantConfig> imageVariants;
    public ConfigItem<List<ImageUploader>> imageUploaders;
    public ConfigItem<MarkerUsage> allowMarkerUsage;
    public ConfigItem<Visibility> commentVisibilityCriteria;
    public ConfigItem<Visibility> markerVisibilityCriteria;
    public ConfigItem<Boolean> imageGlobalKill;
    public ConfigItem<Boolean> defaultCommentVisibilityPreference;

    private Path path;

    private static <T extends Enum<T>> T parseEnum(String name, Class<T> enumClass) {
        return Enum.valueOf(enumClass, name.toUpperCase());
    }

    public void load(Path configPath) throws IOException {
        this.path = configPath;
        JsonObject fileJson = Files.exists(configPath)
                ? JsonParser.parseString(Files.readString(configPath)).getAsJsonObject()
                : new JsonObject();
        JsonObject json = ConfigEnvironment.mergeWithEnvironment(fileJson);

        redisUrl = new ConfigItem<>(fileJson, json, "redisUrl", "", JsonElement::getAsString);
        syncRole = new ConfigItem<>(fileJson, json, "syncRole", SyncRole.HOST, el -> parseEnum(el.getAsString(), SyncRole.class));
        syncNodeId = new ConfigItem<>(fileJson, json, "syncNodeId", -1, JsonElement::getAsInt);
        uplinkUrl = new ConfigItem<>(fileJson, json, "uplinkUrl", "", JsonElement::getAsString);
        uplinkAuthKey = new ConfigItem<>(fileJson, json, "uplinkAuthKey", "", JsonElement::getAsString);
        imageVariants = new ConfigItem<>(fileJson, json, "imageVariants",
                ImageVariantConfig.defaults(),
                el -> ImageVariantConfig.fromJson(el.getAsJsonObject()));
        imageUploaders = new ConfigItem<List<ImageUploader>>(fileJson, json, "imageUploadConfig", () -> ImageUploader.parseUploaderList(List.of()), el -> {
            List<JsonObject> uploaderConfigs = new ArrayList<>();
            if (el.isJsonArray()) {
                for (JsonElement element : el.getAsJsonArray()) {
                    uploaderConfigs.add(element.getAsJsonObject());
                }
            } else if (el.isJsonObject()) {
                uploaderConfigs.add(el.getAsJsonObject());
            }
            return ImageUploader.parseUploaderList(uploaderConfigs);
        });
        allowMarkerUsage = new ConfigItem<>(fileJson, json, "allowMarkerUsage", MarkerUsage.CREATIVE, el -> parseEnum(el.getAsString(), MarkerUsage.class));
        commentVisibilityCriteria = new ConfigItem<>(fileJson, json, "commentVisibilityCriteria", Visibility.PREFERENCE, el -> parseEnum(el.getAsString(), Visibility.class));
        markerVisibilityCriteria = new ConfigItem<>(fileJson, json, "markerVisibilityCriteria", Visibility.ALWAYS, el -> parseEnum(el.getAsString(), Visibility.class));
        imageGlobalKill = new ConfigItem<>(fileJson, json, "imageGlobalKill", false, JsonElement::getAsBoolean);
        defaultCommentVisibilityPreference = new ConfigItem<>(fileJson, json, "defaultCommentVisibilityPreference", false, JsonElement::getAsBoolean);

        if (!Files.exists(configPath)) save(configPath);
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();
        redisUrl.writeJson(json);
        syncRole.writeJson(json);
        syncNodeId.writeJson(json);
        uplinkUrl.writeJson(json);
        uplinkAuthKey.writeJson(json);
        imageVariants.writeJson(json);
        imageUploaders.writeJson(json);
        allowMarkerUsage.writeJson(json);
        commentVisibilityCriteria.writeJson(json);
        markerVisibilityCriteria.writeJson(json);
        imageGlobalKill.writeJson(json);
        defaultCommentVisibilityPreference.writeJson(json);
        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public void save() throws IOException {
        save(this.path);
    }
}
