package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean screenshotIncludeGui = false;
    public boolean screenshotIncludeComments = false;

    public List<ImageUploader> imageUploader;

    public int allowMarkerUsage;
    public int commentVisibilityCriteria;

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
            case "tool_toggle" -> 0;
            case "glass_worn" -> 1;
            default -> 0;
        };
        return config;
    }

    public void tick(float deltaTicks) {
        CommentToolItem.updateInvisibilityTimer(deltaTicks);
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
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeInt(imageUploader.size());
        for (ImageUploader uploader : imageUploader) {
            packet.writeUtf(uploader.serialize().toString());
        }
        packet.writeInt(allowMarkerUsage);
        packet.writeInt(commentVisibilityCriteria);
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
        if ((comment.messageType - 1) >= 4) return true;
        return switch (commentVisibilityCriteria) {
            case 0 -> CommentToolItem.getVisibilityPreference();
            case 1 -> false; // TODO
            default -> false;
        };
    }
}
