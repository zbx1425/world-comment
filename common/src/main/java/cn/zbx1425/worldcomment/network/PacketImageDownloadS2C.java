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
import java.util.Arrays;

public class PacketImageDownloadS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("image_download");

    public static void send(ServerPlayer target, String fileName, byte[] imageData) {
        int totalChunks = (imageData.length + LocalStorageUploader.IMAGE_CHUNK_SIZE - 1) / LocalStorageUploader.IMAGE_CHUNK_SIZE;
        
        for (int i = 0; i < totalChunks; i++) {
            int start = i * LocalStorageUploader.IMAGE_CHUNK_SIZE;
            int end = Math.min(start + LocalStorageUploader.IMAGE_CHUNK_SIZE, imageData.length);
            byte[] chunk = Arrays.copyOfRange(imageData, start, end);
            
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeUtf(fileName);
            buffer.writeInt(imageData.length); // Total size
            buffer.writeInt(i); // Chunk index
            buffer.writeInt(totalChunks); // Total chunks
            buffer.writeInt(chunk.length); // This chunk size
            buffer.writeBytes(chunk);
            ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
        }
    }

    public static void sendNotFound(ServerPlayer target, String fileName) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf(fileName);
        buffer.writeInt(0); // Total size of 0 indicates not found
        buffer.writeInt(0); // Chunk index
        buffer.writeInt(1); // Total chunks
        buffer.writeInt(0); // Chunk size
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {
        private static class ChunkCollector {
            private final byte[] completeData;
            private int receivedChunks = 0;
            private final int totalChunks;

            public ChunkCollector(int totalSize, int totalChunks) {
                this.completeData = new byte[totalSize];
                this.totalChunks = totalChunks;
            }

            public boolean addChunk(int chunkIndex, byte[] chunkData, int offset) {
                System.arraycopy(chunkData, 0, completeData, offset, chunkData.length);
                receivedChunks++;
                return receivedChunks == totalChunks;
            }

            public byte[] getCompleteData() {
                return completeData;
            }
        }

        private static final java.util.Map<String, ChunkCollector> chunkCollectors = new java.util.HashMap<>();

        public static void handle(FriendlyByteBuf buffer) {
            String fileName = buffer.readUtf();
            int totalSize = buffer.readInt();
            
            if (totalSize == 0) {
                LocalStorageUploader.completeDownloadExceptionally(fileName, new IOException("Image not found"));
                return;
            }

            int chunkIndex = buffer.readInt();
            int totalChunks = buffer.readInt();
            int chunkSize = buffer.readInt();
            byte[] chunkData = new byte[chunkSize];
            buffer.readBytes(chunkData);

            ChunkCollector collector;
            synchronized (chunkCollectors) {
                collector = chunkCollectors.computeIfAbsent(fileName,
                        k -> new ChunkCollector(totalSize, totalChunks));
            }

            int offset = chunkIndex * LocalStorageUploader.IMAGE_CHUNK_SIZE;
            boolean isComplete = collector.addChunk(chunkIndex, chunkData, offset);

            if (isComplete) {
                synchronized (chunkCollectors) {
                    chunkCollectors.remove(fileName);
                }
                LocalStorageUploader.completeDownload(fileName, collector.getCompleteData());
            }
        }
    }
} 