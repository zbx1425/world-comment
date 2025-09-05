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

    public List<ImageUploader> imageUploaders;

    public ServerConfig.MarkerUsage allowMarkerUsage;
    public ServerConfig.Visibility commentVisibilityCriteria;
    public ServerConfig.Visibility markerVisibilityCriteria;

    public boolean imageGlobalKill = false;

    public boolean commentVisibilityMask = true;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();
        config.imageUploaders = serverConfig.imageUploaders.value;
        config.allowMarkerUsage = serverConfig.allowMarkerUsage.value;
        config.commentVisibilityCriteria = serverConfig.commentVisibilityCriteria.value;
        config.markerVisibilityCriteria = serverConfig.markerVisibilityCriteria.value;
        config.imageGlobalKill = serverConfig.imageGlobalKill.value;
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
        imageUploaders = ImageUploader.parseUploaderList(uploaderConfigs);
        allowMarkerUsage = packet.readEnum(ServerConfig.MarkerUsage.class);
        commentVisibilityCriteria = packet.readEnum(ServerConfig.Visibility.class);
        markerVisibilityCriteria = packet.readEnum(ServerConfig.Visibility.class);
        imageGlobalKill = packet.readBoolean();
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeInt(imageUploaders.size());
        for (ImageUploader uploader : imageUploaders) {
            packet.writeUtf(uploader.serializeForClient().toString());
        }
        packet.writeEnum(allowMarkerUsage);
        packet.writeEnum(commentVisibilityCriteria);
        packet.writeEnum(markerVisibilityCriteria);
        packet.writeBoolean(imageGlobalKill);
    }

    public boolean canAccessBuildMarkers(Minecraft minecraft) {
        return switch (allowMarkerUsage) {
            case OP -> minecraft.player.hasPermissions(2);
            case CREATIVE -> minecraft.gameMode.getPlayerMode() == GameType.CREATIVE;
            case ALL -> true;
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
        ServerConfig.Visibility criteriaToUse = ((comment.messageType - 1) >= 4) ? markerVisibilityCriteria : commentVisibilityCriteria;
        return switch (criteriaToUse) {
            case ALWAYS -> true;
            case NEVER -> false;
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
