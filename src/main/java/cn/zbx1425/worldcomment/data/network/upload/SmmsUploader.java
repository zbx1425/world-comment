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

public class SmmsUploader extends ImageUploader {

    private final String apiUrl;
    private final String apiToken;

    public SmmsUploader(String id, JsonObject serializedOrConfig) {
        super(id, "smms", serializedOrConfig);
        if (serializedOrConfig.has("apiUrl")) {
            this.apiUrl = serializedOrConfig.get("apiUrl").getAsString();
        } else {
            this.apiUrl = "https://smms.app/api/v2/upload";
        }
        this.apiToken = serializedOrConfig.get("apiToken").getAsString();
    }

    @Override
    public CompletableFuture<UploadResult> uploadImage(byte[] imageData, String filename, CommentAffinityInfo info) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMultipartData body = MimeMultipartData.newBuilder()
                        .withCharset(StandardCharsets.UTF_8)
                        .addFile("smfile", filename, imageData, "image/webp")
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
                        throw new CompletionException(new IOException(
                                "HTTP Error Code " + response.statusCode() + "\n" + response.body()));
                    }
                    JsonObject respObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!respObj.get("success").getAsBoolean()) {
                        if (respObj.get("code").getAsString().equals("image_repeated")) {
                            return new UploadResult(respObj.get("images").getAsString());
                        } else {
                            throw new CompletionException(new IOException("Upload Fail " + response.body()));
                        }
                    } else {
                        return new UploadResult(respObj.get("data").getAsJsonObject().get("url").getAsString());
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
