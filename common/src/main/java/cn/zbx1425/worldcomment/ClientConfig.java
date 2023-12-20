package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.network.upload.ImageUploadConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean isCommentVisible = true;

    public List<ImageUploadConfig> imageUploader;

    public int allowMarkerUsage;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();

        List<ImageUploadConfig> uploaderList = new ArrayList<>();
        for (String uploaderStr : serverConfig.imageUploadConfig.value.split(";")) {
            if (!uploaderStr.contains(":")) continue;
            uploaderList.add(new ImageUploadConfig(uploaderStr));
        }
        uploaderList.add(new ImageUploadConfig(":"));
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
        // isCommentVisible = packet.readBoolean();
        int uploaderCount = packet.readInt();
        imageUploader = new ArrayList<>();
        for (int i = 0; i < uploaderCount; i++) {
            imageUploader.add(new ImageUploadConfig(packet.readUtf()));
        }
        allowMarkerUsage = packet.readInt();
    }

    public void writePacket(FriendlyByteBuf packet) {
        // packet.writeBoolean(isCommentVisible);
        packet.writeInt(imageUploader.size());
        for (ImageUploadConfig uploader : imageUploader) {
            packet.writeUtf(uploader.toString());
        }
        packet.writeInt(allowMarkerUsage);
    }

}
