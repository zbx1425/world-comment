package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ImageDownload {

    private static final Map<String, ImageState> images = new HashMap<>();

    public static AbstractTexture getTexture(ThumbImage image, boolean thumb) {
        String targetUrl = (thumb && !image.thumbUrl.isEmpty()) ? image.thumbUrl : image.url;
        synchronized (images) {
            if (images.containsKey(targetUrl)) return queryTexture(targetUrl);
            images.put(targetUrl, new ImageState());
        }

        Main.IO_EXECUTOR.execute(() -> {
            try {
                byte[] localImageData = getLocalImageData(image.url);
                if (localImageData != null) {
                    applyImageData(targetUrl, localImageData);
                }
            } catch (IOException ex) {
                Main.LOGGER.warn("Cannot read local image {}", image.url, ex);
            }

            downloadImage(targetUrl);
        });

        return queryTexture(targetUrl);
    }

    private static void downloadImage(String url) {
        HttpRequest request = ImageUploader.requestBuilder(URI.create(url))
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .GET()
                .build();
        Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode()));
                    }
                    byte[] imageData = response.body();
                    applyImageData(url, imageData);
                })
                .exceptionally(ex -> {
                    Main.LOGGER.warn("Cannot download image {}", url, ex);
                    synchronized (images) {
                        if (!images.containsKey(url)) return null;
                        images.get(url).failed = true;
                    }
                    return null;
                });
    }

    private static byte[] getLocalImageData(String url) throws IOException {
        Path imageBaseDir = Minecraft.getInstance().gameDirectory.toPath().resolve("worldcomment-images");
        Path imagePath = imageBaseDir.resolve("url-sha1-" + DigestUtils.sha1Hex(url)
            + (url.endsWith(".jpg") ? ".jpg" : ".png"));
        if (Files.exists(imagePath)) {
            return Files.readAllBytes(imagePath);
        }
        return null;
    }

    private static void applyImageData(String url, byte[] pngOrJpgImageData) {
        byte[] imageData = pngOrJpgImageData;
        if (url.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
            // Actually maybe directly construct NativeImage from jpg
            imageData = ImageConvert.toPng(imageData);
        }
        ByteBuffer buffer = OffHeapAllocator.allocate(imageData.length);
        buffer.put(imageData);
        buffer.rewind();
        Minecraft.getInstance().execute(() -> {
            try {
                DynamicTexture dynamicTexture = new DynamicTexture(NativeImage.read(buffer));
                synchronized (images) {
                    if (!images.containsKey(url)) return;
                    images.get(url).texture = dynamicTexture;
                }
            } catch (Throwable ex) {
                Main.LOGGER.warn("Cannot store image " + url, ex);
                synchronized (images) {
                    if (!images.containsKey(url)) return;
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
        synchronized (images) {
            for (Iterator<Map.Entry<String, ImageState>> it = images.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, ImageState> entry = it.next();
                if (currentTime - entry.getValue().queryTime > 60000) {
                    if (entry.getValue().texture != null) entry.getValue().texture.close();
                    it.remove();
                }
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
