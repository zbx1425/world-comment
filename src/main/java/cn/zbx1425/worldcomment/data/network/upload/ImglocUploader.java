package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.MimeMultipartData;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ImglocUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiToken;

    public ImglocUploader(String id, JsonObject serializedOrConfig) {
        super(id, "imgloc", serializedOrConfig);
        if (serializedOrConfig.has("apiUrl")) {
            this.apiUrl = serializedOrConfig.get("apiUrl").getAsString();
        } else {
            this.apiUrl = "https://imgloc.com/api/1/upload";
        }
        this.apiToken = serializedOrConfig.get("apiToken").getAsString();
    }

    @Override
    public boolean hasNativeThumbnail() {
        return true;
    }

    @Override
    public CompletableFuture<UploadResult> uploadImage(byte[] imageData, String filename, CommentAffinityInfo info) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMultipartData body = MimeMultipartData.newBuilder()
                        .withCharset(StandardCharsets.UTF_8)
                        .addFile("source", filename, imageData, "image/webp")
                        .addText("title", "WorldComment from " + info.initiatorName)
                        .addText("description", info.message)
                        .build();
                return ImageUploader.requestBuilder(URI.create(apiUrl))
                        .header("Content-Type", body.getContentType())
                        .header("X-API-Key", apiToken)
                        .POST(body.getBodyPublisher())
                        .build();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Main.IO_EXECUTOR)
                .thenCompose(reqUpload -> Main.HTTP_CLIENT.sendAsync(reqUpload, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() != 200)
                        throw new CompletionException(new IOException(
                                "HTTP Error Code " + response.statusCode() + "\n" + response.body()));
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!respObj.has("success")) {
                        throw new CompletionException(new IOException("Upload Fail " + response.body()));
                    } else {
                        String url = respObj.get("image").getAsJsonObject().get("url").getAsString();
                        String mediumUrl = respObj.get("image").getAsJsonObject()
                                .get("medium").getAsJsonObject().get("url").getAsString();
                        return new UploadResult(url, mediumUrl);
                    }
                });
    }

    @Override
    public JsonObject serializeForClient() {
        JsonObject obj = super.serializeForClient();
        obj.addProperty("apiUrl", apiUrl);
        obj.addProperty("apiToken", apiToken);
        return obj;
    }
}
