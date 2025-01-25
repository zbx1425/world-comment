package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvertServer;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public class PacketImageUploadC2S {

    public static final ResourceLocation IDENTIFIER = Main.id("image_upload");
    public static final int MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB limit
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static class ClientLogics {
        public static void send(CommentEntry comment, byte[] imageBytes) {
            if (imageBytes.length > MAX_IMAGE_SIZE) return;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(comment.id);
            buffer.writeInt(imageBytes.length);
            buffer.writeLong(comment.timestamp);
            buffer.writeBytes(imageBytes);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        long jobId = buffer.readLong();
        int length = buffer.readInt();
        if (length > MAX_IMAGE_SIZE) return;

        long timestamp = buffer.readLong();
        String baseFileName = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(FILE_DATE_FORMAT)
                + "-" + String.format("%016X", jobId) + "-" + initiator.getGameProfile().getName();

        byte[] imageData = new byte[length];
        buffer.readBytes(imageData);

        try {
            Path serverImagePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("world-comment").resolve("image");
            if (!Files.exists(serverImagePath)) {
                Files.createDirectories(serverImagePath);
            }

            byte[] fullData = ImageConvertServer.toJpegScaled(imageData, ImageUploader.IMAGE_MAX_WIDTH);
            String fullFileName = baseFileName + ".jpg";
            Path fullFile = serverImagePath.resolve(fullFileName);
            Files.write(fullFile, fullData);

            byte[] thumbData = ImageConvertServer.toJpegScaled(imageData, ImageUploader.THUMBNAIL_MAX_WIDTH);
            String thumbFileName = baseFileName + ".thumb.jpg";
            Path jpgFile = serverImagePath.resolve(thumbFileName);
            Files.write(jpgFile, thumbData);

            // 发送成功响应，包含原图和缩略图的URL
            ThumbImage image = new ThumbImage(
                LocalStorageUploader.URL_PREFIX + fullFileName,
                    LocalStorageUploader.URL_PREFIX + thumbFileName
            );
            PacketImageUploadS2C.send(initiator, jobId, image);
        } catch (IOException e) {
            PacketImageUploadS2C.sendException(initiator, jobId, e);
            Main.LOGGER.error("Failed to save uploaded image", e);
        }
    }
} 