package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class ImageDownload {

    private static final Map<String, ImageState> images = new HashMap<>();

    public static ImageState getTexture(ThumbImage image, boolean thumb) {
        if (image.url.isEmpty() || MainClient.CLIENT_CONFIG.imageGlobalKill) return ImageState.BLANK;
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
                    return;
                }
            } catch (IOException ex) {
                Main.LOGGER.warn("Cannot read local image {}", image.url, ex);
            }

            downloadImage(targetUrl);
        });

        return queryTexture(targetUrl);
    }

    private static void downloadImage(String url) {
        if (url.startsWith(LocalStorageUploader.URL_PREFIX)) {
            LocalStorageUploader.downloadImage(url)
                    .thenAccept(imageData -> applyImageData(url, imageData))
                    .exceptionally(ex -> {
                        Main.LOGGER.warn("Cannot download image {}", url, ex);
                        synchronized (images) {
                            if (!images.containsKey(url)) return null;
                            images.get(url).failed = true;
                        }
                        return null;
                    });
        } else {
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
    }

    private static byte[] getLocalImageData(String url) throws IOException {
        Path imageBaseDir = Minecraft.getInstance().gameDirectory.toPath().resolve("worldcomment-images");
        Path imagePath = imageBaseDir.resolve(getCacheFileName(url));
        if (Files.exists(imagePath)) {
            return Files.readAllBytes(imagePath);
        }
        return null;
    }

    private static void applyImageData(String url, byte[] pngOrJpgImageData) {
        byte[] imageData = pngOrJpgImageData;
        if (url.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
            // Actually maybe directly construct NativeImage from jpg
            imageData = ImageConvertClient.toPng(imageData);
        }
        ByteBuffer buffer = OffHeapAllocator.allocate(imageData.length);
        buffer.put(imageData);
        buffer.rewind();
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage pixels = NativeImage.read(buffer);
                DynamicTexture dynamicTexture = new DynamicTexture(pixels);
                synchronized (images) {
                    ImageState sink = images.get(url);
                    if (sink == null) return;
                    sink.texture = dynamicTexture;
                    sink.width = pixels.getWidth();
                    sink.height = pixels.getHeight();
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

    private static ImageState queryTexture(String url) {
        synchronized (images) {
            ImageState state = images.get(url);
            state.onQuery();
            return state;
        }
    }

    public static void purgeUnused() {
        long currentTime = System.currentTimeMillis();
        synchronized (images) {
            Iterator<Map.Entry<String, ImageState>> iterator = images.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ImageState> entry = iterator.next();
                if (currentTime - entry.getValue().lastQueryTime > 60000) {
                    if (entry.getValue().texture != null) {
                        entry.getValue().texture.close();
                    }
                    iterator.remove();
                }
            }
        }
    }

    public static class ImageState {
        public DynamicTexture texture;
        public int width = 16, height = 9;
        public boolean failed;
        public boolean blank;
        public long lastQueryTime;

        private ImageState(boolean blank) {
            this.blank = blank;
        }

        public ImageState() {
            this(false);
        }

        public void onQuery() {
            lastQueryTime = System.currentTimeMillis();
        }

        public AbstractTexture getFriendlyTexture(TextureManager textureManager) {
            if (failed) {
                return textureManager.getTexture(Main.id("textures/gui/placeholder-failed.png"));
            } else if (texture != null) {
                return texture;
            } else if (blank) {
                return textureManager.getTexture(Main.id("textures/gui/placeholder-blank.png"));
            } else {
                return textureManager.getTexture(Main.id("textures/gui/placeholder-loading.png"));
            }
        }

        public static final ImageState BLANK = new ImageState(true);
    }

    public static String getCacheFileName(String url) {
        byte[] urlBytes = url.getBytes(StandardCharsets.UTF_8);
        String hash = DigestUtils.sha1Hex(urlBytes);
        String extension = url.toLowerCase().endsWith(".jpg") ? ".jpg" : ".png";
        return String.format("url-sha1-%s%s", hash, extension);
    }

}
