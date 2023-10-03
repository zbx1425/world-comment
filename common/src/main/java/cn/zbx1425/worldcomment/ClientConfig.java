package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.network.upload.ImageUploadConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {

    public boolean isCommentVisible;

    public List<ImageUploadConfig> imageUploader;

    public static ClientConfig fromServerConfig(ServerConfig serverConfig) {
        ClientConfig config = new ClientConfig();
        List<ImageUploadConfig> uploaderList = new ArrayList<>();
        for (String uploaderStr : serverConfig.imageUploadConfig.value.split(";")) {
            uploaderList.add(new ImageUploadConfig(uploaderStr));
        }
        uploaderList.add(new ImageUploadConfig(":"));
        config.imageUploader = uploaderList;
        return config;
    }

    public ClientConfig() {

    }

    public ClientConfig(FriendlyByteBuf packet) {
        isCommentVisible = packet.readBoolean();
        int uploaderCount = packet.readInt();
        imageUploader = new ArrayList<>();
        for (int i = 0; i < uploaderCount; i++) {
            imageUploader.add(new ImageUploadConfig(packet.readUtf()));
        }
    }

    public void writePacket(FriendlyByteBuf packet) {
        packet.writeBoolean(isCommentVisible);
        packet.writeInt(imageUploader.size());
        for (ImageUploadConfig uploader : imageUploader) {
            packet.writeUtf(uploader.toString());
        }
    }

}
