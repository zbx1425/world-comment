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

    public LskyUploader(JsonObject config) {
        this.apiUrl = config.get("apiUrl").getAsString();
        this.apiToken = config.get("apiToken").getAsString();
        this.strategyId = config.has("strategyId") ? config.get("strategyId").getAsInt() : null;
        this.albumId = config.has("albumId") ? config.get("albumId").getAsInt() : null;
    }

    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            MimeMultipartData.Builder bodyBuilder = MimeMultipartData.newBuilder()
                    .withCharset(StandardCharsets.UTF_8)
                    .addFile("file", "WorldComment from " + comment.initiatorName + ".jpg",
                            ImageConvertClient.toJpegScaled(imageBytes, IMAGE_MAX_WIDTH), "application/octet-stream");
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
        return json;
    }
}
