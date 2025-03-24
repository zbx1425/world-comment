package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ImageDump {

    private static long lastRequestNonce = 0;
    private static String lastRequestDirName = "";

    public static void requestDumpComments(String dirName) {
        lastRequestNonce = ServerWorldData.SNOWFLAKE.nextId();
        lastRequestDirName = dirName;
        PacketCollectionRequestC2S.ClientLogics.sendLatest(0, Integer.MAX_VALUE, lastRequestNonce);
    }

    public static void handleDumpResponse(List<CommentEntry> comments, long nonce) {
        if (nonce != lastRequestNonce) return;
        Path storeDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("worldcomment-images").resolve(lastRequestDirName);
        try {
            if (!Files.isDirectory(storeDir)) Files.createDirectories(storeDir);
        } catch (IOException ex) {
            Main.LOGGER.error("Cannot create image dump directory", ex);
            return;
        }

        Main.IO_EXECUTOR.execute(() -> {
            for (int i = 0; i < comments.size(); i++) {
                int finalI = i;
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("WorldComment: Downloading " + (finalI + 1) + "/" + comments.size()),
                            true);
                    }
                });
                CommentEntry comment = comments.get(i);
                String targetUrl = comment.image.url;
                if (targetUrl.isEmpty()) continue;
                Path filePath = storeDir.resolve(ImageDownload.getCacheFileName(targetUrl));
                if (!Files.exists(filePath)) {
                    try {
                        byte[] imageData = Main.HTTP_CLIENT.send(
                                ImageUploader.requestBuilder(URI.create(targetUrl))
                                        .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray()).body();
                        Files.write(filePath, imageData);
                    } catch (IOException | InterruptedException ex) {
                        Main.LOGGER.warn("Cannot download image {}", targetUrl, ex);
                    }
                }
            }
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("WorldComment: Download finished"),
                            false);
                }
            });
        });
    }
}
