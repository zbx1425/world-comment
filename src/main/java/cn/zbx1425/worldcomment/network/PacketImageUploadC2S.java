package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.platform.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.CommentImage;
import cn.zbx1425.worldcomment.data.network.ImageConvertServer;
import cn.zbx1425.worldcomment.data.network.upload.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class PacketImageUploadC2S {

    public static final Identifier IDENTIFIER = Main.id("image_upload");

    public static class ClientLogics {
        public static void send(long jobId, byte[] imageBytes) {
            if (imageBytes.length > LocalStorageUploader.IMAGE_MAX_SIZE) {
                Main.LOGGER.warn("Image too large: {}B", imageBytes.length);
                return;
            }

            int totalChunks = (imageBytes.length + LocalStorageUploader.IMAGE_CHUNK_SIZE - 1) / LocalStorageUploader.IMAGE_CHUNK_SIZE;

            for (int i = 0; i < totalChunks; i++) {
                int start = i * LocalStorageUploader.IMAGE_CHUNK_SIZE;
                int end = Math.min(start + LocalStorageUploader.IMAGE_CHUNK_SIZE, imageBytes.length);
                byte[] chunk = Arrays.copyOfRange(imageBytes, start, end);

                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                buffer.writeLong(jobId);
                buffer.writeInt(imageBytes.length);
                buffer.writeLong(System.currentTimeMillis());
                buffer.writeInt(i);
                buffer.writeInt(totalChunks);
                buffer.writeInt(chunk.length);
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
                CommentAffinityInfo info = new CommentAffinityInfo(
                        initiator.getGameProfile().id(), initiator.getGameProfile().name());

                LocalDateTime now = LocalDateTime.now();
                String subDir = now.format(DateTimeFormatter.ofPattern("yyMM"));

                Path serverImagePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                        .resolve("worldcomment").resolve("image").resolve(subDir);
                if (!Files.exists(serverImagePath)) {
                    Files.createDirectories(serverImagePath);
                }

                ImageVariantConfig variantConfig = Main.SERVER_CONFIG.imageVariants.value;
                String baseName = UrlTemplate.transform(
                        ImageUploader.DEFAULT_FILENAME_FORMAT, jobId, info, ImageFilePurpose.SOURCE);

                String sourceFileName = subDir + "/" + baseName + ".webp";
                Files.write(serverImagePath.resolve(baseName + ".webp"), completeImageData);
                String sourceUrl = LocalStorageUploader.URL_PREFIX + sourceFileName;

                String detailUrl = "";
                if (variantConfig.hasArchive()) {
                    byte[] detailWebp = ImageConvertServer.anyToWebp(completeImageData, variantConfig.detail());
                    String mediumBase = UrlTemplate.transform(
                            ImageUploader.DEFAULT_FILENAME_FORMAT, jobId, info, ImageFilePurpose.MEDIUM);
                    String mediumFileName = subDir + "/" + mediumBase + ".webp";
                    Files.write(serverImagePath.resolve(mediumBase + ".webp"), detailWebp);
                    detailUrl = LocalStorageUploader.URL_PREFIX + mediumFileName;
                }

                String thumbUrl = "";
                if (variantConfig.hasThumbnail()) {
                    byte[] thumbWebp = ImageConvertServer.anyToWebp(completeImageData, variantConfig.thumbnail());
                    String thumbBase = UrlTemplate.transform(
                            ImageUploader.DEFAULT_FILENAME_FORMAT, jobId, info, ImageFilePurpose.THUMBNAIL);
                    String thumbFileName = subDir + "/" + thumbBase + ".webp";
                    Files.write(serverImagePath.resolve(thumbBase + ".webp"), thumbWebp);
                    thumbUrl = LocalStorageUploader.URL_PREFIX + thumbFileName;
                }

                CommentImage image = new CommentImage("local", sourceUrl, detailUrl, thumbUrl);
                PacketImageUploadS2C.send(initiator, jobId, image);
            } catch (IOException e) {
                PacketImageUploadS2C.sendException(initiator, jobId, e);
                Main.LOGGER.error("Failed to save uploaded image", e);
            }
        }
    }
}
