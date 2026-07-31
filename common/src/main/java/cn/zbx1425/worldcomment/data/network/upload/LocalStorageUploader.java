package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.network.CommentImage;
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
    private static final Map<Long, CompletableFuture<CommentImage>> pendingUploads = new HashMap<>();
    private static final Map<String, CompletableFuture<byte[]>> pendingDownloads = new HashMap<>();
    private static final long TIMEOUT_SECONDS = 30;

    public static final int IMAGE_MAX_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int IMAGE_CHUNK_SIZE = 28 * 1024; // 28KB chunks

    private LocalStorageUploader() {
        super("local", "local", null);
    }

    public static LocalStorageUploader getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<UploadResult> uploadImage(byte[] imageData, String filename, CommentAffinityInfo info) {
        throw new UnsupportedOperationException("LocalStorageUploader uses uploadForCommentImage instead");
    }

    public CompletableFuture<CommentImage> uploadForCommentImage(long jobId, byte[] imageData) {
        CompletableFuture<CommentImage> future = new CompletableFuture<>();
        synchronized (pendingUploads) {
            CompletableFuture<CommentImage> existing = pendingUploads.get(jobId);
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            if (existing != null) {
                pendingUploads.remove(jobId);
            }
            pendingUploads.put(jobId, future);
        }

        PacketImageUploadC2S.ClientLogics.send(jobId, imageData);
        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    synchronized (pendingUploads) {
                        pendingUploads.remove(jobId);
                    }
                });
    }

    public static void completeUpload(long jobId, CommentImage image) {
        CompletableFuture<CommentImage> future;
        synchronized (pendingUploads) {
            future = pendingUploads.remove(jobId);
        }
        if (future != null) {
            future.complete(image);
        }
    }

    public static void completeUploadExceptionally(long jobId, Throwable ex) {
        CompletableFuture<CommentImage> future;
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
}
