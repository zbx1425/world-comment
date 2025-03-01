package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvertClient;
import cn.zbx1425.worldcomment.data.network.ImageConvertServer;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class PacketImageUploadC2S {

    public static final ResourceLocation IDENTIFIER = Main.id("image_upload");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static class ClientLogics {
        public static void send(CommentEntry comment, byte[] imageBytes) {
            // Convert to JPEG before uploading to avoid hitting vanilla 2MB packet size limit
            byte[] jpgData = ImageConvertClient.toJpegScaled(imageBytes, ImageUploader.IMAGE_MAX_WIDTH);
            if (jpgData.length > LocalStorageUploader.IMAGE_MAX_SIZE) {
                Main.LOGGER.warn("Image too large: {}B", jpgData.length);
                return;
            }

            int totalChunks = (jpgData.length + LocalStorageUploader.IMAGE_CHUNK_SIZE - 1) / LocalStorageUploader.IMAGE_CHUNK_SIZE;
            
            for (int i = 0; i < totalChunks; i++) {
                int start = i * LocalStorageUploader.IMAGE_CHUNK_SIZE;
                int end = Math.min(start + LocalStorageUploader.IMAGE_CHUNK_SIZE, jpgData.length);
                byte[] chunk = Arrays.copyOfRange(jpgData, start, end);
                
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                buffer.writeLong(comment.id);
                buffer.writeInt(jpgData.length); // Total size
                buffer.writeLong(comment.timestamp);
                buffer.writeInt(i); // Chunk index
                buffer.writeInt(totalChunks); // Total chunks
                buffer.writeInt(chunk.length); // This chunk size
                buffer.writeBytes(chunk);
                
                ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
            }
        }
    }

    private static class ChunkCollector {
        private final byte[] completeData;
        private int receivedChunks = 0;
        private final int totalChunks;
        private final long timestamp;

        public ChunkCollector(int totalSize, int totalChunks, long timestamp) {
            this.completeData = new byte[totalSize];
            this.totalChunks = totalChunks;
            this.timestamp = timestamp;
        }

        public boolean addChunk(int chunkIndex, byte[] chunkData, int offset) {
            System.arraycopy(chunkData, 0, completeData, offset, chunkData.length);
            receivedChunks++;
            return receivedChunks == totalChunks;
        }

        public byte[] getCompleteData() {
            return completeData;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private static final java.util.Map<Long, ChunkCollector> chunkCollectors = new java.util.HashMap<>();

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        long jobId = buffer.readLong();
        int totalSize = buffer.readInt();
        if (totalSize > LocalStorageUploader.IMAGE_MAX_SIZE) return;

        long timestamp = buffer.readLong();
        int chunkIndex = buffer.readInt();
        int totalChunks = buffer.readInt();
        int chunkSize = buffer.readInt();
        
        byte[] chunkData = new byte[chunkSize];
        buffer.readBytes(chunkData);

        ChunkCollector collector;
        synchronized (chunkCollectors) {
            collector = chunkCollectors.computeIfAbsent(jobId,
                    k -> new ChunkCollector(totalSize, totalChunks, timestamp));
        }

        int offset = chunkIndex * LocalStorageUploader.IMAGE_CHUNK_SIZE;
        boolean isComplete = collector.addChunk(chunkIndex, chunkData, offset);

        if (isComplete) {
            synchronized (chunkCollectors) {
                chunkCollectors.remove(jobId);
            }

            try {
                byte[] completeImageData = collector.getCompleteData();
                String baseFileName = Instant.ofEpochMilli(collector.getTimestamp())
                        .atOffset(ZoneOffset.UTC).format(FILE_DATE_FORMAT)
                        + "-" + String.format("%016x", jobId) + "-" + initiator.getGameProfile().getName();

                Path serverImagePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("world-comment").resolve("image");
                if (!Files.exists(serverImagePath)) {
                    Files.createDirectories(serverImagePath);
                }

                String fullFileName = baseFileName + ".jpg";
                Path fullFile = serverImagePath.resolve(fullFileName);
                Files.write(fullFile, completeImageData);

                byte[] thumbData = ImageConvertServer.toJpegScaled(completeImageData, ImageUploader.THUMBNAIL_MAX_WIDTH);
                String thumbFileName = baseFileName + ".thumb.jpg";
                Path jpgFile = serverImagePath.resolve(thumbFileName);
                Files.write(jpgFile, thumbData);

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
} 