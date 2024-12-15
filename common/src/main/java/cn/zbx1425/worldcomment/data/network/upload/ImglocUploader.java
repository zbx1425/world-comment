package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
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

    public ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("source", "image.png", imageBytes, "image/png")
                .addText("title", "WorldComment from " + comment.initiatorName)
                .addText("description", comment.message)
                .build();
        HttpRequest reqUpload = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", body.getContentType())
                .header("User-Agent",
                        "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://www.zbx1425.cn")
                .header("X-API-Key", apiToken)
                .POST(body.getBodyPublisher())
                .build();
        HttpResponse<String> respUpload = HTTP_CLIENT.send(reqUpload, HttpResponse.BodyHandlers.ofString());
        if (respUpload.statusCode() != 200)
            throw new IOException("Upload HTTP " + respUpload.statusCode() + "\n" + respUpload.body());
        JsonObject respObj = JsonParser.parseString(respUpload.body()).getAsJsonObject();
        if (!respObj.has("success")) {
            throw new IOException("Upload Fail " + respUpload.body());
        } else {
            return new ThumbImage(
                    respObj.get("image").getAsJsonObject().get("url").getAsString(),
                    respObj.get("image").getAsJsonObject().get("medium").getAsJsonObject().get("url").getAsString()
            );
        }
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
