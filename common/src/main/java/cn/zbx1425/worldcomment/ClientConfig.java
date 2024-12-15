package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean isCommentVisible = true;

    public boolean screenshotIncludeGui = false;
    public boolean screenshotIncludeComments = true;

    public List<ImageUploader> imageUploader;

    public int allowMarkerUsage;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();

        List<ImageUploader> uploaderList = new ArrayList<>();
        try {
            JsonElement rootElement = JsonParser.parseString(serverConfig.imageUploadConfig.value);
            if (rootElement.isJsonArray()) {
                for (JsonElement element : rootElement.getAsJsonArray()) {
                    uploaderList.add(ImageUploader.getUploader(element.getAsJsonObject()));
                }
            } else if (rootElement.isJsonObject()) {
                uploaderList.add(ImageUploader.getUploader(rootElement.getAsJsonObject()));
            }
        } catch (Exception ex) {
            Main.LOGGER.error("Failed to parse image upload config", ex);
        }
        uploaderList.add(ImageUploader.NoopUploader.INSTANCE);
        config.imageUploader = uploaderList;

        config.allowMarkerUsage = switch (serverConfig.allowMarkerUsage.value) {
            case "op" -> 0;
            case "creative" -> 1;
            case "all" -> 2;
            default -> 1;
        };

        return config;
    }

    public ClientConfig() {

    }

    public void readPacket(FriendlyByteBuf packet) {
        int uploaderCount = packet.readInt();
        imageUploader = new ArrayList<>();
        for (int i = 0; i < uploaderCount; i++) {
            imageUploader.add(ImageUploader.getUploader(JsonParser.parseString(packet.readUtf()).getAsJsonObject()));
        }
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
