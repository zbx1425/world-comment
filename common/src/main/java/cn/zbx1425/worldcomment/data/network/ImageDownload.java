package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.codec.digest.DigestUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ImageDownload {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final Map<String, ImageState> images = new HashMap<>();

    public static AbstractTexture getTexture(ThumbImage image, boolean thumb) {
        try {
            byte[] localImageData = getLocalImageData(image.url);
            if (localImageData != null) {
                if (!images.containsKey(image.url)) {
                    images.put(image.url, new ImageState());
                    applyImageData(image.url, localImageData);
                }
                return queryTexture(image.url);
            }
        } catch (IOException ex) {
            Main.LOGGER.warn("Cannot read local image " + image.url, ex);
        }

        String targetUrl = (thumb && !image.thumbUrl.isEmpty()) ? image.thumbUrl : image.url;
        if (!images.containsKey(targetUrl)) {
            images.put(targetUrl, new ImageState());
            downloadImage(targetUrl);
        }
        return queryTexture(targetUrl);
    }

    private static void downloadImage(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent",
                        "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://www.zbx1425.cn")
                .header("X-Minecraft-Username", Minecraft.getInstance().getUser().getName())
                .header("X-Minecraft-UUID", Minecraft.getInstance().getUser().getProfileId().toString())
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .GET()
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        Main.LOGGER.warn("Cannot download image {}: HTTP {}", url, response.statusCode());
                        synchronized (images) {
                            images.get(url).failed = true;
                        }
                        return;
                    }
                    byte[] imageData = response.body();
                    applyImageData(url, imageData);
                })
                .exceptionally(ex -> {
                    Main.LOGGER.warn("Cannot download image {}", url, ex);
                    synchronized (images) {
                        images.get(url).failed = true;
                    }
                    return null;
                });
    }

    private static byte[] getLocalImageData(String url) throws IOException {
        Path imageBaseDir = Minecraft.getInstance().gameDirectory.toPath().resolve("worldcomment-images");
        if (!Files.isDirectory(imageBaseDir)) return null;
        try (Stream<Path> imageDirs = Files.list(imageBaseDir)) {
            for (Path imageDir : imageDirs.toArray(Path[]::new)) {
                Path imagePath = imageDir.resolve("url-sha1-" + DigestUtils.sha1Hex(url)
                    + (url.endsWith(".jpg") ? ".jpg" : ".png"));
                if (Files.exists(imagePath)) {
                    return Files.readAllBytes(imagePath);
                }
            }
        }
        return null;
    }

    private static void applyImageData(String url, byte[] pngOrJpgImageData) {
        Minecraft.getInstance().execute(() -> {
            byte[] imageData = pngOrJpgImageData;
            if (url.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
                // Actually maybe directly construct NativeImage from jpg
                imageData = ImageConvert.toPng(imageData);
            }
            DynamicTexture dynamicTexture;
            ByteBuffer buffer = OffHeapAllocator.allocate(imageData.length);
            try {
                buffer.put(imageData);
                buffer.rewind();
                dynamicTexture = new DynamicTexture(NativeImage.read(buffer));
                synchronized (images) {
                    images.get(url).texture = dynamicTexture;
                }
            } catch (Throwable ex) {
                Main.LOGGER.warn("Cannot store image " + url, ex);
                synchronized (images) {
                    images.get(url).failed = true;
                }
            } finally {
                OffHeapAllocator.free(buffer);
            }
        });
    }

    private static AbstractTexture queryTexture(String url) {
        synchronized (images) {
            ImageState state = images.get(url);
            state.onQuery();
            if (state.texture != null) return state.texture;
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            if (state.failed) return textureManager.getTexture(
                    Main.id("textures/gui/placeholder-failed.png"));
            return textureManager.getTexture(
                    Main.id("textures/gui/placeholder-loading.png"));
        }
    }

    public static void purgeUnused() {
        long currentTime = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, ImageState>> it = images.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ImageState> entry = it.next();
            if (currentTime - entry.getValue().queryTime > 60000) {
                if (entry.getValue().texture != null) entry.getValue().texture.close();
                it.remove();
            }
        }
    }

    private static class ImageState {
        public long queryTime;
        public DynamicTexture texture;
        public boolean failed;

        public ImageState() {
            queryTime = System.currentTimeMillis();
        }

        public void onQuery() {
            queryTime = System.currentTimeMillis();
        }
    }

}
