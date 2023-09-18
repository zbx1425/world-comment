package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageUpload {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static String apiUrl = "https://imgloc.com/api/1/upload";
    private static String apiToken = "chv_WdW_afa0889c65f7afcdbd13fc2528d9b9793d193a9a8e16c78f964be1e731cd10e9884d806a1bf767d45cafc5952216827d65541a31174b5acc45f988e6a2210885";

    public static ThumbImage uploadImage(Path imagePath, CommentEntry comment) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("source", imagePath.getFileName().toString(), Files.readAllBytes(imagePath), "image/png")
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
}
