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

public class SmmsUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiToken;

    public SmmsUploader(JsonObject config) {
        if (config.has("apiUrl")) {
            this.apiUrl = config.get("apiUrl").getAsString();
        } else {
            this.apiUrl = "https://smms.app/api/v2/upload";
        }
        this.apiToken = config.get("apiToken").getAsString();
    }

    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        String initiatorName = comment.initiatorName.isBlank() ? "anonymous" : comment.initiatorName;
        CompletableFuture<String> fullSizeUrlFuture = uploadImage(imageBytes, IMAGE_MAX_WIDTH,
                "sender-" + initiatorName + ".jpg");
        CompletableFuture<String> thumbnailFuture = uploadImage(imageBytes, THUMBNAIL_MAX_WIDTH,
                "sender-" + initiatorName + ".thumb.jpg");
        return CompletableFuture.allOf(fullSizeUrlFuture, thumbnailFuture).thenApply(ignored ->
                new ThumbImage(fullSizeUrlFuture.join(), thumbnailFuture.join()));
    }

    private CompletableFuture<String> uploadImage(byte[] imageBytes, int maxWidth, String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMultipartData body = MimeMultipartData.newBuilder()
                        .withCharset(StandardCharsets.UTF_8)
                        .addFile("smfile", fileName, ImageConvertClient.toJpegScaled(imageBytes, maxWidth), "image/jpg")
                        .build();
                return ImageUploader.requestBuilder(URI.create(apiUrl))
                        .header("Content-Type", body.getContentType())
                        .header("Authorization", "Basic " + apiToken)
                        .POST(body.getBodyPublisher())
                        .build();
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        }, Main.IO_EXECUTOR)
                .thenCompose(reqUpload -> Main.HTTP_CLIENT.sendAsync(reqUpload, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode() + "\n" + response.body()));
                    }
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!respObj.get("success").getAsBoolean()) {
                        if (respObj.get("code").getAsString().equals("image_repeated")) {
                            return respObj.get("images").getAsString();
                        } else {
                            throw new CompletionException(new IOException("Upload Fail " + response.body()));
                        }
                    } else {
                        return respObj.get("data").getAsJsonObject().get("url").getAsString();
                    }
                });
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", "smms");
        obj.addProperty("apiUrl", apiUrl);
        obj.addProperty("apiToken", apiToken);
        return obj;
    }
}
