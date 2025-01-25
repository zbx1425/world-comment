package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean isCommentVisible = true;

    public boolean screenshotIncludeGui = false;
    public boolean screenshotIncludeComments = true;

    public float commentHideTimer = 0f;

    public List<ImageUploader> imageUploader;

    public int allowMarkerUsage;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();
        config.imageUploader = ImageUploader.parseUploaderList(serverConfig.parseUploaderConfig());
        config.allowMarkerUsage = switch (serverConfig.allowMarkerUsage.value) {
            case "op" -> 0;
            case "creative" -> 1;
            case "all" -> 2;
            default -> 1;
        };
        return config;
    }

    public void tick(float deltaTicks) {
        if (commentHideTimer > 0) {
            commentHideTimer -= deltaTicks;
            if (commentHideTimer <= 0) {
                isCommentVisible = true;
            }
        }
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
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeInt(imageUploader.size());
        for (ImageUploader uploader : imageUploader) {
            packet.writeUtf(uploader.serialize().toString());
        }
        packet.writeInt(allowMarkerUsage);
    }

}
