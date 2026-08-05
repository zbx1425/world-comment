package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.mojang.blaze3d.platform.NativeImage;
import net.jpountz.xxhash.XXHashFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ImageDownload {

    private static final Map<String, ImageState> images = new HashMap<>();

    public static ImageState getTexture(String resolvedUrl) {
        if (resolvedUrl == null || resolvedUrl.isEmpty()
                || MainClient.CLIENT_CONFIG.serverIssuedConfig.imageGlobalKill) {
            return ImageState.BLANK;
        }
        synchronized (images) {
            if (images.containsKey(resolvedUrl)) return queryTexture(resolvedUrl);
            images.put(resolvedUrl, new ImageState());
        }

        Main.IO_EXECUTOR.execute(() -> {
            try {
                byte[] cachedData = readFromDiskCache(resolvedUrl);
                if (cachedData != null) {
                    applyImageData(resolvedUrl, cachedData, false)
                            .exceptionally(ex -> {
                                Main.LOGGER.warn("Failed to load image {} from disk", resolvedUrl, ex);

                                // Cached image was corrupted. Reset it to loading state and try to download again.
                                synchronized (images) {
                                    ImageState state = images.get(resolvedUrl);
                                    if (state != null) state.failed = false;
                                }
                                Main.IO_EXECUTOR.execute(() -> {
                                    try {
                                        Files.deleteIfExists(getCachePath(resolvedUrl));
                                    } catch (IOException ignored) {}
                                    downloadImage(resolvedUrl);
                                });
                                return null;
                            });
                    return;
                }
            } catch (IOException ex) {
                Main.LOGGER.warn("Cannot read cached image {}", resolvedUrl, ex);
            }

            downloadImage(resolvedUrl);
        });

        return queryTexture(resolvedUrl);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static CompletableFuture<Void> downloadImage(String url) {
        if (url.startsWith(LocalStorageUploader.URL_PREFIX)) {
            return LocalStorageUploader.downloadImage(url)
                    .thenCompose(imageData -> applyImageData(url, imageData, true))
                    .exceptionally(ex -> {
                        Main.LOGGER.warn("Cannot load image {}", url, ex);
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
            return Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenCompose(response -> {
                        if (response.statusCode() != 200) {
                            throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode()));
                        }
                        return applyImageData(url, response.body(), true);
                    })
                    .exceptionally(ex -> {
                        Main.LOGGER.warn("Cannot load image {}", url, ex);
                        synchronized (images) {
                            if (!images.containsKey(url)) return null;
                            images.get(url).failed = true;
                        }
                        return null;
                    });
        }
    }

    // --- Disk cache ---

    public static Path getCachePath(String url) {
        Path baseDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("worldcomment").resolve("image");

        String host;
        String pathStr;

        if (url.startsWith(LocalStorageUploader.URL_PREFIX)) {
            host = MainClient.CLIENT_CONFIG.perServerPreference.serverKey + ".worldcomment";
            pathStr = url.substring(LocalStorageUploader.URL_PREFIX.length());
        } else {
            URI uri = URI.create(url).normalize();
            host = uri.getHost();
            if (host == null) host = "unknown";
            pathStr = uri.getPath();
            if (pathStr == null || pathStr.isEmpty()) pathStr = "index";
            if (pathStr.startsWith("/")) pathStr = pathStr.substring(1);

            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
                long hash = XXHashFactory.fastestInstance().hash64()
                        .hash(queryBytes, 0, queryBytes.length, 0);
                String hashHex = String.format("%016x", hash);
                int dotIndex = pathStr.lastIndexOf('.');
                int slashIndex = pathStr.lastIndexOf('/');
                if (dotIndex > slashIndex && dotIndex > 0) {
                    pathStr = pathStr.substring(0, dotIndex) + "@" + hashHex + pathStr.substring(dotIndex);
                } else {
                    pathStr = pathStr + "@" + hashHex;
                }
            }
        }

        host = sanitizePathComponent(host);
        String[] parts = pathStr.split("/");
        Path result = baseDir.resolve(host);
        for (String part : parts) {
            if (part.isEmpty() || part.equals("..") || part.equals(".")) continue;
            result = result.resolve(sanitizePathComponent(part));
        }

        if (!result.normalize().startsWith(baseDir.normalize())) {
            throw new IllegalArgumentException("Path traversal? " + url);
        }

        return result;
    }

    private static String sanitizePathComponent(String component) {
        return component
                .replace("*", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_")
                .replace(":", "_");
    }

    public static byte[] readFromDiskCache(String url) throws IOException {
        Path cachePath = getCachePath(url);
        if (Files.exists(cachePath)) {
            return Files.readAllBytes(cachePath);
        }
        return null;
    }

    public static void writeToDiskCache(String url, byte[] rawImageData) {
        try {
            Path cachePath = getCachePath(url);
            if (Files.exists(cachePath)) return;
            Files.createDirectories(cachePath.getParent());
            Path tmpFile = cachePath.resolveSibling(cachePath.getFileName() + ".tmp");
            Files.write(tmpFile, rawImageData);
            Files.move(tmpFile, cachePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Main.LOGGER.warn("Cannot write image to cache {}", url, ex);
        }
    }

    // --- Image loading ---

    private static CompletableFuture<Void> applyImageData(String url, byte[] rawImageData, boolean updateCache) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            byte[] imageData = rawImageData;
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".webp") || isWebpMagicBytes(rawImageData)) {
                imageData = ImageConvertClient.webpToPng(imageData);
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                imageData = ImageConvertClient.jpegToPng(imageData);
            }
            ByteBuffer buffer = OffHeapAllocator.allocate(imageData.length);
            buffer.put(imageData);
            buffer.rewind();
            Minecraft.getInstance().execute(() -> {
                try {
                    NativeImage pixels = NativeImage.read(buffer);
#if MC_VERSION >= "12006"
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> url, pixels);
#else
                    DynamicTexture dynamicTexture = new DynamicTexture(pixels);
#endif
                    synchronized (images) {
                        ImageState sink = images.get(url);
                        if (sink == null) {
                            future.complete(null);
                            return;
                        }
                        sink.texture = dynamicTexture;
                        sink.width = pixels.getWidth();
                        sink.height = pixels.getHeight();
                        sink.failed = false;
                    }
                    if (updateCache) {
                        Main.IO_EXECUTOR.execute(() -> writeToDiskCache(url, rawImageData));
                    }
                    future.complete(null);
                } catch (Throwable ex) {
                    Main.LOGGER.warn("Cannot decode image " + url, ex);
                    synchronized (images) {
                        if (images.containsKey(url)) images.get(url).failed = true;
                    }
                    future.completeExceptionally(ex);
                } finally {
                    OffHeapAllocator.free(buffer);
                }
            });
        } catch (Throwable ex) {
            Main.LOGGER.warn("Cannot decode image " + url, ex);
            synchronized (images) {
                if (images.containsKey(url)) images.get(url).failed = true;
            }
            future.completeExceptionally(ex);
        }
        return future;
    }

    static boolean isWebpMagicBytes(byte[] data) {
        return data.length > 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }

    static boolean verifyImageIntegrity(String url, byte[] rawBytes) {
        byte[] imageData = rawBytes;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webp") || isWebpMagicBytes(rawBytes)) {
            imageData = ImageConvertClient.webpToPng(imageData);
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            imageData = ImageConvertClient.jpegToPng(imageData);
        }
        ByteBuffer buffer = OffHeapAllocator.allocate(imageData.length);
        buffer.put(imageData);
        buffer.rewind();
        try {
            NativeImage pixels = NativeImage.read(buffer);
            pixels.close();
            return true;
        } catch (Throwable ex) {
            return false;
        } finally {
            OffHeapAllocator.free(buffer);
        }
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
}
