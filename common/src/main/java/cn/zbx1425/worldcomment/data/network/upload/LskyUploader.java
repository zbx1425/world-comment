package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvertClient;
import cn.zbx1425.worldcomment.data.network.MimeMultipartData;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LskyUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiToken;
    private final Integer strategyId;
    private final Integer albumId;
    private final String cdnImageTransform;
    private final Boolean localThumbGeneration;

    public LskyUploader(JsonObject config) {
        this.apiUrl = config.get("apiUrl").getAsString();
        this.apiToken = config.get("apiToken").getAsString();
        this.strategyId = config.has("strategyId") ? config.get("strategyId").getAsInt() : null;
        this.albumId = config.has("albumId") ? config.get("albumId").getAsInt() : null;
        this.cdnImageTransform = config.has("cdnImageTransform") ? config.get("cdnImageTransform").getAsString() : null;
        this.localThumbGeneration = config.has("localThumbGeneration") ? config.get("localThumbGeneration").getAsBoolean() : null;
    }

    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        String initiatorName = comment.initiatorName.isBlank() ? "anonymous" : comment.initiatorName;
        if (localThumbGeneration) {
            CompletableFuture<ThumbImage> fullSizeUrlFuture = uploadImage(imageBytes, IMAGE_MAX_WIDTH,
                    "sender-" + initiatorName + ".jpg");
            CompletableFuture<ThumbImage> thumbnailFuture = uploadImage(imageBytes, THUMBNAIL_MAX_WIDTH,
                    "sender-" + initiatorName + ".thumb.jpg");
            return CompletableFuture.allOf(fullSizeUrlFuture, thumbnailFuture).thenApply(ignored ->
                    new ThumbImage(fullSizeUrlFuture.join().url, thumbnailFuture.join().url));
        } else {
            return uploadImage(imageBytes, IMAGE_MAX_WIDTH, "sender-" + initiatorName + ".jpg")
                    .thenApply(originalThumb -> {
                        String thumbUrl;
                        if (cdnImageTransform != null) {
                            try {
                                URI uri = URI.create(originalThumb.url);
                                String path = uri.getPath();
                                thumbUrl = originalThumb.url.replace(path, cdnImageTransform
                                        .replace("{thumbWidth}", Integer.toString(ImageUploader.THUMBNAIL_MAX_WIDTH))
                                        .replace("{quality100}", Integer.toString(ImageUploader.THUMBNAIL_QUALITY))
                                        .replace("{quality1}", String.format("%.2f", ImageUploader.THUMBNAIL_QUALITY / 100f))
                                        .replace("{path}", path.substring(1)));
                            } catch (Exception e) {
                                Main.LOGGER.error("Error transforming thumbnail URL", e);
                                thumbUrl = originalThumb.thumbUrl;
                            }
                        } else {
                            thumbUrl = originalThumb.thumbUrl;
                        }
                        return new ThumbImage(originalThumb.url, thumbUrl);
                    });
        }
    }

    private CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, int maxWidth, String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMultipartData.Builder bodyBuilder = MimeMultipartData.newBuilder()
                        .withCharset(StandardCharsets.UTF_8)
                        .addFile("file", fileName, ImageConvertClient.toJpegScaled(imageBytes, maxWidth), "application/octet-stream");
                if (strategyId != null) bodyBuilder.addText("strategy_id", Integer.toString(strategyId));
                if (albumId != null) bodyBuilder.addText("album_id", Integer.toString(albumId));
                MimeMultipartData body = bodyBuilder.build();
                return ImageUploader.requestBuilder(URI.create(apiUrl))
                        .header("Content-Type", body.getContentType())
                        .header("Authorization", "Bearer " + apiToken)
                        .POST(body.getBodyPublisher())
                        .build();
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        }, Main.IO_EXECUTOR)
                .thenCompose(request -> Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode() + "\n" + response.body()));
                    }
                    JsonObject linkObj = JsonParser.parseString(response.body()).getAsJsonObject()
                            .get("data").getAsJsonObject().get("links").getAsJsonObject();
                    return new ThumbImage(linkObj.get("url").getAsString(), linkObj.get("thumbnail_url").getAsString());
                });
    }

    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("service", "lsky");
        json.addProperty("apiUrl", apiUrl);
        json.addProperty("apiToken", apiToken);
        if (strategyId != null) json.addProperty("strategyId", strategyId);
        if (albumId != null) json.addProperty("albumId", albumId);
        if (cdnImageTransform != null) json.addProperty("cdnImageTransform", cdnImageTransform);
        if (localThumbGeneration != null) json.addProperty("localThumbGeneration", localThumbGeneration);
        return json;
    }
}
