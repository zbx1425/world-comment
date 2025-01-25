package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PacketImageDownloadS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("image_download");

    public static void send(ServerPlayer target, String fileName, byte[] imageData) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf(fileName);
        buffer.writeInt(imageData.length);
        buffer.writeBytes(imageData);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static void sendNotFound(ServerPlayer target, String fileName) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf(fileName);
        buffer.writeInt(0);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {
        public static void handle(FriendlyByteBuf buffer) {
            String fileName = buffer.readUtf();
            int length = buffer.readInt();
            if (length == 0) {
                LocalStorageUploader.completeDownloadExceptionally(fileName, new IOException("Image not found"));
                return;
            }
            byte[] imageData = new byte[length];
            buffer.readBytes(imageData);
            // 完成LocalStorageUploader中的下载Future
            LocalStorageUploader.completeDownload(fileName, imageData);
        }
    }
} 