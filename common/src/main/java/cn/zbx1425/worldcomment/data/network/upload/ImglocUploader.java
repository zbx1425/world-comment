package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvert;
import cn.zbx1425.worldcomment.data.network.MimeMultipartData;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ImglocUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiToken;

    public ImglocUploader(JsonObject config) {
        if (config.has("apiUrl")) {
            this.apiUrl = config.get("apiUrl").getAsString();
        } else {
            this.apiUrl = "https://imgloc.com/api/1/upload";
        }
        this.apiToken = config.get("apiToken").getAsString();
    }

    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("source", "WorldComment from " + comment.initiatorName + ".jpg",
                        ImageConvert.toJpegScaled(imageBytes, IMAGE_MAX_WIDTH), "image/jpg")
                .addText("title", "WorldComment from " + comment.initiatorName)
                .addText("description", comment.message)
                .build();
        HttpRequest reqUpload = ImageUploader.requestBuilder(URI.create(apiUrl))
                .header("Content-Type", body.getContentType())
                .header("X-API-Key", apiToken)
                .POST(body.getBodyPublisher())
                .build();
        return HTTP_CLIENT.sendAsync(reqUpload, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200)
                        throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode() + "\n" + response.body()));
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!respObj.has("success")) {
                        throw new CompletionException(new IOException("Upload Fail " + response.body()));
                    } else {
                        return new ThumbImage(
                                respObj.get("image").getAsJsonObject().get("url").getAsString(),
                                respObj.get("image").getAsJsonObject().get("medium").getAsJsonObject().get("url").getAsString()
                        );
                    }
                });
    }

    @Override
    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", "imgloc");
        obj.addProperty("apiUrl", apiUrl);
        obj.addProperty("apiToken", apiToken);
        return obj;
    }
}
