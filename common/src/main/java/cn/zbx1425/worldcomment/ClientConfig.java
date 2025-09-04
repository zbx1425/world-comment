package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean screenshotKeyTakeOver = true;

    public boolean screenshotIncludeGui = false;
    public boolean screenshotIncludeComments = false;
    public boolean commentVisibilityPreference = true;

    public List<ImageUploader> imageUploader;

    public int allowMarkerUsage;
    public int commentVisibilityCriteria;
    public int markerVisibilityCriteria;

    public boolean imageGlobalKill = false;

    public boolean commentVisibilityMask = true;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();
        config.imageUploader = ImageUploader.parseUploaderList(serverConfig.parseUploaderConfig());
        config.allowMarkerUsage = switch (serverConfig.allowMarkerUsage.value) {
            case "op" -> 0;
            case "creative" -> 1;
            case "all" -> 2;
            default -> 1;
        };
        config.commentVisibilityCriteria = switch (serverConfig.commentVisibilityCriteria.value) {
            case "preference" -> 0;
            case "always" -> 999;
            case "never" -> -1;
            default -> 0;
        };
        config.markerVisibilityCriteria = switch (serverConfig.markerVisibilityCriteria.value) {
            case "preference" -> 0;
            case "always" -> 999;
            case "never" -> -1;
            default -> 0;
        };
        config.imageGlobalKill = serverConfig.imageGlobalKill.value.equals("true");
        return config;
    }

    public ClientConfig() {

    }

    public void readPacket(FriendlyByteBuf packet) {
        int uploaderCount = packet.readInt();
        List<JsonObject> uploaderConfigs = new ArrayList<>();
        for (int i = 0; i < uploaderCount; i++) {
            uploaderConfigs.add(JsonParser.parseString(packet.readUtf()).getAsJsonObject());
        }
        imageUploader = ImageUploader.parseUploaderList(uploaderConfigs);
        allowMarkerUsage = packet.readInt();
        commentVisibilityCriteria = packet.readInt();
        imageGlobalKill = packet.readBoolean();
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeInt(imageUploader.size());
        for (ImageUploader uploader : imageUploader) {
            packet.writeUtf(uploader.serialize().toString());
        }
        packet.writeInt(allowMarkerUsage);
        packet.writeInt(commentVisibilityCriteria);
        packet.writeBoolean(imageGlobalKill);
    }

    public boolean canAccessBuildMarkers(Minecraft minecraft) {
        return switch (allowMarkerUsage) {
            case 0 -> minecraft.player.hasPermissions(2);
            case 1 -> minecraft.gameMode.getPlayerMode() == GameType.CREATIVE;
            case 2 -> true;
            default -> false;
        };
    }

    public boolean isCommentVisible(Minecraft minecraft, CommentEntry comment) {
        if (!commentVisibilityMask) return false;
        if (comment.initiator.equals(minecraft.player.getGameProfile().getId())
            && (System.currentTimeMillis() - comment.timestamp) < 30000) {
            // Show a newly placed comment to its owner for 30 seconds.
            return true;
        }
        int criteriaToUse = ((comment.messageType - 1) >= 4) ? markerVisibilityCriteria : commentVisibilityCriteria;
        return switch (criteriaToUse) {
            case 999 -> true;
            case -1 -> false;
            default -> commentVisibilityPreference;
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
