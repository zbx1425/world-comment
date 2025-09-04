package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvertClient;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class S3PreSignedUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiAuthKey;
    private final String cdnImageTransform;
    private final boolean localThumbGeneration;

    public S3PreSignedUploader(JsonObject config) {
        this.apiUrl = config.get("apiUrl").getAsString();
        this.apiAuthKey = config.get("apiAuthKey").getAsString();
        this.cdnImageTransform = config.has("cdnImageTransform") ? config.get("cdnImageTransform").getAsString() : null;
        this.localThumbGeneration = config.has("localThumbGeneration") && config.get("localThumbGeneration").getAsBoolean();
    }

    @Override
    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        return getPreSignedUrlPair(comment)
                .thenCompose(preSignedUrls -> {
                    if (localThumbGeneration) {
                        CompletableFuture<Void> fullSizeUpload = uploadToS3(preSignedUrls.url, imageBytes, IMAGE_MAX_WIDTH);
                        CompletableFuture<Void> thumbUpload = uploadToS3(preSignedUrls.thumbUrl, imageBytes, THUMBNAIL_MAX_WIDTH);
                        return CompletableFuture.allOf(fullSizeUpload, thumbUpload)
                                .thenApply(v -> new ThumbImage(stripQuery(preSignedUrls.url), stripQuery(preSignedUrls.thumbUrl)));
                    } else {
                        return uploadToS3(preSignedUrls.url, imageBytes, IMAGE_MAX_WIDTH)
                                .thenApply(v -> {
                                    String publicUrl = stripQuery(preSignedUrls.url);
                                    String thumbUrl;
                                    if (cdnImageTransform != null) {
                                        thumbUrl = transformUrl(publicUrl);
                                    } else {
                                        thumbUrl = stripQuery(preSignedUrls.thumbUrl);
                                    }
                                    return new ThumbImage(publicUrl, thumbUrl);
                                });
                    }
                });
    }

    private CompletableFuture<ThumbImage> getPreSignedUrlPair(CommentEntry comment) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject objectToSend = comment.toJson().deepCopy();
            objectToSend.remove("message");
            objectToSend.remove("image");
            objectToSend.addProperty("requestTimestamp", Instant.now().getEpochSecond());
            byte[] postDataBytes = objectToSend.toString().getBytes(StandardCharsets.UTF_8);

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(postDataBytes));

            if (apiAuthKey != null && !apiAuthKey.isEmpty()) {
                byte[] signatureBytes = Hashing
                        .hmacSha1(apiAuthKey.getBytes(StandardCharsets.UTF_8))
                        .hashBytes(postDataBytes).asBytes();
                builder.header("Authorization", "NEX-HMAC-SHA1 Signature=" + Base64.getEncoder().encodeToString(signatureBytes));
            }
            return builder.build();
        }, Main.IO_EXECUTOR)
                .thenCompose(request -> Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("Failed to get presigned URL: "
                                + response.statusCode() + "\n" + response.body()));
                    }
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    return new ThumbImage(respObj);
                });
    }

    private CompletableFuture<Void> uploadToS3(String presignedUrl, byte[] imageBytes, int maxWidth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] scaledImage = ImageConvertClient.toJpegScaled(imageBytes, maxWidth);
                return HttpRequest.newBuilder(URI.create(presignedUrl))
                        .header("Content-Type", "image/jpeg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(scaledImage))
                        .build();
            } catch (IllegalStateException e) {
                throw new CompletionException(e);
            }
        }, Main.IO_EXECUTOR)
                .thenCompose(request -> Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding()))
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("S3 upload failed: " + response.statusCode()));
                    }
                });
    }

    private String stripQuery(String url) {
        if (url == null) return null;
        int queryIndex = url.indexOf('?');
        return (queryIndex == -1) ? url : url.substring(0, queryIndex);
    }

    private String transformUrl(String originalUrl) {
        try {
            URI uri = URI.create(originalUrl);
            String path = uri.getPath();
            return originalUrl.replace(path, cdnImageTransform
                    .replace("{thumbWidth}", Integer.toString(ImageUploader.THUMBNAIL_MAX_WIDTH))
                    .replace("{quality100}", Integer.toString(ImageUploader.THUMBNAIL_QUALITY))
                    .replace("{quality1}", String.format("%.2f", ImageUploader.THUMBNAIL_QUALITY / 100f))
                    .replace("{path}", path.startsWith("/") ? path.substring(1) : path));
        } catch (Exception e) {
            Main.LOGGER.error("Error transforming thumbnail URL", e);
            return originalUrl;
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("service", "s3PreSigned");
        json.addProperty("apiUrl", apiUrl);
        json.addProperty("apiAuthKey", apiAuthKey);
        if (cdnImageTransform != null) json.addProperty("cdnImageTransform", cdnImageTransform);
        if (localThumbGeneration) json.addProperty("localThumbGeneration", true);
        return json;
    }
}
