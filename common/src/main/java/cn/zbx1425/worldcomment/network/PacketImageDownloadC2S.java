package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PacketImageDownloadC2S {

    public static final ResourceLocation IDENTIFIER = Main.id("image_download");

    public static class ClientLogics {
        public static void send(String fileName) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeUtf(fileName);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        String fileName = buffer.readUtf();
        try {
            Path imagePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("world-comment").resolve("image")
                    .resolve(fileName);
            if (Files.exists(imagePath)) {
                byte[] imageData = Files.readAllBytes(imagePath);
                PacketImageDownloadS2C.send(initiator, fileName, imageData);
            } else {
                PacketImageDownloadS2C.sendNotFound(initiator, fileName);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to read image file", e);
        }
    }
} 