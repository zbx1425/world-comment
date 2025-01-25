package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.network.PacketImageDownloadC2S;
import cn.zbx1425.worldcomment.network.PacketImageUploadC2S;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LocalStorageUploader extends ImageUploader {

    private static final LocalStorageUploader INSTANCE = new LocalStorageUploader();
    public static final String URL_PREFIX = "file://worldcomment/";
    private static final Map<Long, CompletableFuture<ThumbImage>> pendingUploads = new HashMap<>();
    private static final Map<String, CompletableFuture<byte[]>> pendingDownloads = new HashMap<>();
    private static final long TIMEOUT_SECONDS = 30;

    private LocalStorageUploader() {}

    public static LocalStorageUploader getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        CompletableFuture<ThumbImage> future = new CompletableFuture<>();
        synchronized (pendingUploads) {
            CompletableFuture<ThumbImage> existing = pendingUploads.get(comment.id);
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            if (existing != null) {
                pendingUploads.remove(comment.id);
            }
            pendingUploads.put(comment.id, future);
        }

        PacketImageUploadC2S.ClientLogics.send(comment, imageBytes);
        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    synchronized (pendingUploads) {
                        pendingUploads.remove(comment.id);
                    }
                });
    }

    public static void completeUpload(long jobId, ThumbImage image) {
        CompletableFuture<ThumbImage> future;
        synchronized (pendingUploads) {
            future = pendingUploads.remove(jobId);
        }
        if (future != null) {
            future.complete(image);
        }
    }

    public static void completeUploadExceptionally(long jobId, Throwable ex) {
        CompletableFuture<ThumbImage> future;
        synchronized (pendingUploads) {
            future = pendingUploads.remove(jobId);
        }
        if (future != null) {
            future.completeExceptionally(ex);
        }
    }

    public static CompletableFuture<byte[]> downloadImage(String url) {
        if (!url.startsWith(URL_PREFIX)) {
            throw new IllegalArgumentException("Not a WorldComment URL: " + url);
        }
        String fileName = url.substring(URL_PREFIX.length());
        synchronized (pendingDownloads) {
            CompletableFuture<byte[]> existing = pendingDownloads.get(url);
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            if (existing != null) {
                pendingDownloads.remove(url);
            }
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            pendingDownloads.put(url, future);
            PacketImageDownloadC2S.ClientLogics.send(fileName);
            return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((result, ex) -> {
                        synchronized (pendingDownloads) {
                            pendingDownloads.remove(url);
                        }
                    });
        }
    }

    public static void completeDownload(String fileName, byte[] imageData) {
        String url = URL_PREFIX + fileName;
        CompletableFuture<byte[]> future;
        synchronized (pendingDownloads) {
            future = pendingDownloads.remove(url);
        }
        if (future != null) {
            future.complete(imageData);
        }
    }

    public static void completeDownloadExceptionally(String fileName, Throwable ex) {
        String url = URL_PREFIX + fileName;
        CompletableFuture<byte[]> future;
        synchronized (pendingDownloads) {
            future = pendingDownloads.remove(url);
        }
        if (future != null) {
            future.completeExceptionally(ex);
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", "local");
        return obj;
    }

    public static LocalStorageUploader deserialize(JsonObject config) {
        return getInstance();
    }
} 