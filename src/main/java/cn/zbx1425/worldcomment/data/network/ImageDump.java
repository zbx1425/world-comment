package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ImageDump {

    private static long lastRequestNonce = 0;

    public static void requestDumpComments() {
        lastRequestNonce = ServerWorldData.SNOWFLAKE.nextId();
        PacketCollectionRequestC2S.ClientLogics.sendLatest(0, Integer.MAX_VALUE, lastRequestNonce);
    }

    public static void handleDumpResponse(List<CommentEntry> comments, long nonce) {
        if (nonce != lastRequestNonce) return;

        Main.IO_EXECUTOR.execute(() -> {
            int successCount = 0;
            int failCount = 0;
            List<String> failedUrls = new ArrayList<>();

            for (int i = 0; i < comments.size(); i++) {
                int finalI = i;
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.sendOverlayMessage(
                            Component.literal("WorldComment: Downloading " + (finalI + 1) + "/" + comments.size()));
                    }
                });
                CommentEntry comment = comments.get(i);
                String targetUrl = comment.image.sourceUrl;
                if (targetUrl.isEmpty()) continue;

                try {
                    if (ImageDownload.readFromDiskCache(targetUrl) != null) {
                        successCount++;
                        continue;
                    }
                } catch (IOException ex) {
                    Main.LOGGER.warn("Cannot read cached image {}", targetUrl, ex);
                }

                try {
                    byte[] rawBytes;
                    if (targetUrl.startsWith(LocalStorageUploader.URL_PREFIX)) {
                        rawBytes = LocalStorageUploader.downloadImage(targetUrl).join();
                    } else {
                        URI uri = URI.create(targetUrl);
                        if ("file".equals(uri.getScheme())) continue;
                        HttpResponse<byte[]> response = Main.HTTP_CLIENT.send(
                                ImageUploader.requestBuilder(uri)
                                        .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray());
                        if (response.statusCode() != 200) {
                            throw new IOException("HTTP Error Code " + response.statusCode());
                        }
                        rawBytes = response.body();
                    }

                    if (ImageDownload.verifyImageIntegrity(targetUrl, rawBytes)) {
                        ImageDownload.writeToDiskCache(targetUrl, rawBytes);
                        successCount++;
                    } else {
                        failCount++;
                        failedUrls.add(targetUrl);
                    }
                } catch (Exception ex) {
                    Main.LOGGER.warn("Cannot download image {}", targetUrl, ex);
                    failCount++;
                    failedUrls.add(targetUrl);
                }
            }

            int finalSuccessCount = successCount;
            int finalFailCount = failCount;
            List<String> finalFailedUrls = List.copyOf(failedUrls);
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player == null) return;
                if (finalFailCount == 0) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("WorldComment: Image dump finished, " + finalSuccessCount + " images cached."));
                } else {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.literal("WorldComment: Image dump finished. " + finalSuccessCount + " succeeded, " + finalFailCount + " failed."));
                    for (String failedUrl : finalFailedUrls) {
                        Minecraft.getInstance().player.sendSystemMessage(
                                Component.literal("  Failed: " + failedUrl));
                    }
                }
            });
        });
    }
}
