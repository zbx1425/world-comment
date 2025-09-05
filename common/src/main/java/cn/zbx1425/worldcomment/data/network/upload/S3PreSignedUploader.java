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

    public S3PreSignedUploader(JsonObject serializedOrConfig) {
        super("s3PreSigned", serializedOrConfig);
        this.apiUrl = serializedOrConfig.get("apiUrl").getAsString();
        this.apiAuthKey = serializedOrConfig.get("apiAuthKey").getAsString();
        this.cdnImageTransform = serializedOrConfig.has("cdnImageTransform") ? serializedOrConfig.get("cdnImageTransform").getAsString() : null;
    }

    @Override
    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        return requestPreSign(comment)
                .thenCompose(preSign -> {
                    if (cdnImageTransform == null) {
                        CompletableFuture<Void> fullSizeUpload = uploadToS3(preSign.upload.url, imageBytes, IMAGE_MAX_WIDTH);
                        CompletableFuture<Void> thumbUpload = uploadToS3(preSign.upload.thumbUrl, imageBytes, THUMBNAIL_MAX_WIDTH);
                        return CompletableFuture.allOf(fullSizeUpload, thumbUpload)
                                .thenApply(v -> new ThumbImage(preSign.access.url, preSign.access.thumbUrl));
                    } else {
                        return uploadToS3(preSign.upload.url, imageBytes, IMAGE_MAX_WIDTH)
                                .thenApply(v -> {
                                    String publicUrl = preSign.access.url;
                                    String thumbUrl= transformUrl(publicUrl);
                                    return new ThumbImage(publicUrl, thumbUrl);
                                });
                    }
                });
    }

    private CompletableFuture<PreSignResponse> requestPreSign(CommentEntry comment) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject objectToSend = comment.toJson().deepCopy();
            objectToSend.remove("message");
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
                        throw new CompletionException(new IOException("Failed to pre-sign: "
                                + response.statusCode() + "\n" + response.body()));
                    }
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    return new PreSignResponse(respObj);
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
    public JsonObject serializeForClient() {
        JsonObject json = super.serializeForClient();
        json.addProperty("apiUrl", apiUrl);
        json.addProperty("apiAuthKey", apiAuthKey);
        if (cdnImageTransform != null) json.addProperty("cdnImageTransform", cdnImageTransform);
        return json;
    }

    private record PreSignResponse(ThumbImage upload, ThumbImage access) {
        public PreSignResponse(JsonObject response) {
            this(new ThumbImage(response.get("upload").getAsJsonObject()), new ThumbImage(response.get("access").getAsJsonObject()));
        }
    }
}
