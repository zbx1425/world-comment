package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.data.network.upload.CdnTransformConfig;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.ImageVariantConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientConfig {

    public static class ServerIssuedConfig {
        public List<ImageUploader> imageUploaders;
        public ImageVariantConfig imageVariants;
        public Map<String, CdnTransformConfig> uploaderCdnConfigs;
        public ServerConfig.MarkerUsage allowMarkerUsage;
        public ServerConfig.Visibility commentVisibilityCriteria;
        public ServerConfig.Visibility markerVisibilityCriteria;
        public boolean imageGlobalKill;
        public boolean screenshotKeyTriggersComment;
        public boolean defaultCommentVisibilityPreference;

        public ServerIssuedConfig() {
            imageUploaders = ImageUploader.parseUploaderList(List.of());
            imageVariants = ImageVariantConfig.defaults();
            uploaderCdnConfigs = new HashMap<>();
            allowMarkerUsage = ServerConfig.MarkerUsage.OP;
            commentVisibilityCriteria = ServerConfig.Visibility.PREFERENCE;
            markerVisibilityCriteria = ServerConfig.Visibility.ALWAYS;
            imageGlobalKill = false;
            screenshotKeyTriggersComment = true;
            defaultCommentVisibilityPreference = true;
        }

        public ServerIssuedConfig(FriendlyByteBuf packet) {
            int uploaderCount = packet.readInt();
            List<JsonObject> uploaderConfigs = new ArrayList<>();
            for (int i = 0; i < uploaderCount; i++) {
                uploaderConfigs.add(JsonParser.parseString(packet.readUtf()).getAsJsonObject());
            }
            imageUploaders = ImageUploader.parseUploaderList(uploaderConfigs);

            imageVariants = ImageVariantConfig.readPacket(packet);

            int cdnCount = packet.readInt();
            uploaderCdnConfigs = new HashMap<>(cdnCount);
            for (int i = 0; i < cdnCount; i++) {
                String uploaderId = packet.readUtf();
                CdnTransformConfig cdnConfig = CdnTransformConfig.readPacket(packet);
                uploaderCdnConfigs.put(uploaderId, cdnConfig);
            }

            allowMarkerUsage = packet.readEnum(ServerConfig.MarkerUsage.class);
            commentVisibilityCriteria = packet.readEnum(ServerConfig.Visibility.class);
            markerVisibilityCriteria = packet.readEnum(ServerConfig.Visibility.class);
            imageGlobalKill = packet.readBoolean();
            screenshotKeyTriggersComment = packet.readBoolean();
            defaultCommentVisibilityPreference = packet.readBoolean();
        }

        public ServerIssuedConfig(ServerConfig serverConfig) {
            imageUploaders = serverConfig.imageUploaders.value;
            imageVariants = serverConfig.imageVariants.value;
            uploaderCdnConfigs = new HashMap<>();
            for (ImageUploader uploader : imageUploaders) {
                if (uploader.hasCdnTransform()) {
                    uploaderCdnConfigs.put(uploader.id, uploader.cdnConfig);
                }
            }
            allowMarkerUsage = serverConfig.allowMarkerUsage.value;
            commentVisibilityCriteria = serverConfig.commentVisibilityCriteria.value;
            markerVisibilityCriteria = serverConfig.markerVisibilityCriteria.value;
            imageGlobalKill = serverConfig.imageGlobalKill.value;
            screenshotKeyTriggersComment = serverConfig.screenshotKeyTriggersComment.value;
            defaultCommentVisibilityPreference = serverConfig.defaultCommentVisibilityPreference.value;
        }

        public void writePacket(FriendlyByteBuf packet) {
            packet.writeInt(imageUploaders.size());
            for (ImageUploader uploader : imageUploaders) {
                packet.writeUtf(uploader.serializeForClient().toString());
            }

            imageVariants.writePacket(packet);

            packet.writeInt(uploaderCdnConfigs.size());
            for (var entry : uploaderCdnConfigs.entrySet()) {
                packet.writeUtf(entry.getKey());
                entry.getValue().writePacket(packet);
            }

            packet.writeEnum(allowMarkerUsage);
            packet.writeEnum(commentVisibilityCriteria);
            packet.writeEnum(markerVisibilityCriteria);
            packet.writeBoolean(imageGlobalKill);
            packet.writeBoolean(screenshotKeyTriggersComment);
            packet.writeBoolean(defaultCommentVisibilityPreference);
        }
    }

    public static class TransientPreference {
        public boolean screenshotIncludeGui = false;
        public boolean screenshotIncludeComments = false;
        public boolean commentVisibilityMask = true;
    }

    public static class PerServerPreference {
        public UUID serverKey;
        public String serverIp = "";
        public boolean isDirty = false;
        public boolean commentVisibilityPreference;

        public PerServerPreference() {
            serverKey = new UUID(0L, 0L);
            commentVisibilityPreference = false;
        }

        public PerServerPreference(UUID serverKey, String serverIp, ServerIssuedConfig serverIssuedConfig) {
            this.serverKey = serverKey;
            this.serverIp = serverIp;
            commentVisibilityPreference = serverIssuedConfig.defaultCommentVisibilityPreference;
        }

        public PerServerPreference(UUID serverKey, String serverIp, JsonObject serialized) {
            this.serverKey = serverKey;
            this.serverIp = serverIp;
            commentVisibilityPreference = serialized.has("commentVisibilityPreference")
                    && serialized.get("commentVisibilityPreference").getAsBoolean();
        }

        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("serverIp", serverIp);
            obj.addProperty("commentVisibilityPreference", commentVisibilityPreference);
            return obj;
        }
    }

    public ServerIssuedConfig serverIssuedConfig = new ServerIssuedConfig();
    public TransientPreference transientPreference = new TransientPreference();
    public PerServerPreference perServerPreference = new PerServerPreference();

    public void load(UUID serverKey, String serverIp) {
        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("world-comment-client.json");
        try {
            JsonObject root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
            if (root.has("perServerPreferences")) {
                JsonObject perServerPrefs = root.getAsJsonObject("perServerPreferences");
                if (perServerPrefs.has(serverKey.toString())) {
                    perServerPreference = new PerServerPreference(serverKey, serverIp,
                            perServerPrefs.get(serverKey.toString()).getAsJsonObject());
                } else {
                    perServerPreference = new PerServerPreference(serverKey, serverIp, serverIssuedConfig);
                }
            } else {
                perServerPreference = new PerServerPreference(serverKey, serverIp, serverIssuedConfig);
            }
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load perServerPreference", e);
            perServerPreference = new PerServerPreference(serverKey, serverIp, serverIssuedConfig);
        }
    }

    public void save() {
        if (!perServerPreference.isDirty) return;
        perServerPreference.isDirty = false;
        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("world-comment-client.json");
        try {
            JsonObject root;
            if (Files.exists(configPath)) {
                root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
            } else {
                root = new JsonObject();
            }
            JsonObject perServerPrefs;
            if (root.has("perServerPreferences")) {
                perServerPrefs = root.getAsJsonObject("perServerPreferences");
            } else {
                perServerPrefs = new JsonObject();
                root.add("perServerPreferences", perServerPrefs);
            }
            perServerPrefs.add(perServerPreference.serverKey.toString(), perServerPreference.serialize());
            Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception e) {
            Main.LOGGER.error("Failed to save perServerPreference", e);
        }
    }

    public boolean canAccessBuildMarkers(Minecraft minecraft) {
        return switch (serverIssuedConfig.allowMarkerUsage) {
            case OP -> minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case CREATIVE -> minecraft.gameMode.getPlayerMode() == GameType.CREATIVE;
            case ALL -> true;
            default -> false;
        };
    }

    public boolean isCommentVisible(Minecraft minecraft, CommentEntry comment) {
        if (!transientPreference.commentVisibilityMask) return false;
        if (comment.initiator.equals(minecraft.player.getGameProfile().id())
            && (System.currentTimeMillis() - comment.timestamp) < 30000) {
            return true;
        }
        ServerConfig.Visibility criteriaToUse = (comment.messageType >= EmojiRegistry.HIGH_EMOJI_BASE_ID) ? serverIssuedConfig.markerVisibilityCriteria : serverIssuedConfig.commentVisibilityCriteria;
        return switch (criteriaToUse) {
            case ALWAYS -> true;
            case NEVER -> false;
            default -> perServerPreference.commentVisibilityPreference;
        };
    }

    float accumulatedDeltaTicks = 0;

    public void tick(float deltaTicks, float partialTick) {
        accumulatedDeltaTicks += deltaTicks;
        if (accumulatedDeltaTicks < 2) return;
        ImageDownload.purgeUnused();
        ClientWorldData.INSTANCE.tick();
        ClientRayPicking.tick(partialTick, 20);
        accumulatedDeltaTicks = 0;
    }
}
