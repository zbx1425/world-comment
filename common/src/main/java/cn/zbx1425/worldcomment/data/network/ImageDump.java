package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageDump {

    private static long lastRequestNonce = 0;
    private static String lastRequestDirName = "";

    private static final Executor NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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

        NETWORK_EXECUTOR.execute(() -> {
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
                Path filePath = storeDir.resolve("url-sha1-" + DigestUtils.sha1Hex(targetUrl)
                        + (targetUrl.endsWith(".jpg") ? ".jpg" : ".png"));
                if (!Files.exists(filePath)) {
                    try {
                        byte[] imageData = HTTP_CLIENT.send(
                                HttpRequest.newBuilder(URI.create(targetUrl))
                                        .GET()
                                        .header("User-Agent",
                                                "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://www.zbx1425.cn")
                                        .build(),
                                HttpResponse.BodyHandlers.ofByteArray()).body();
                        Files.write(filePath, imageData);
                    } catch (IOException | InterruptedException ex) {
                        Main.LOGGER.warn("Cannot download image " + targetUrl, ex);
                    }
                }
            }
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("WorldComment: Download finished"),
                            true);
                }
            });
        });
    }
}
