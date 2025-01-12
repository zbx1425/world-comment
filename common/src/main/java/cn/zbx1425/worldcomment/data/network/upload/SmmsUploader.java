package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvert;
import cn.zbx1425.worldcomment.data.network.MimeMultipartData;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) throws IOException, InterruptedException {
        int thumbWidth = 256;
        BufferedImage fullSizeImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        String fullSizeUrl = uploadImage(ImageConvert.toJpegScaled(imageBytes, IMAGE_MAX_WIDTH),
                "WorldComment from " + comment.initiatorName + ".jpg");
        String thumbUrl;
        if (fullSizeImage.getWidth() < thumbWidth) {
            thumbUrl = fullSizeUrl;
        } else {
            thumbUrl = uploadImage(ImageConvert.toJpegScaled(imageBytes, THUMBNAIL_MAX_WIDTH),
                    "WorldComment from " + comment.initiatorName + ".thumb.jpg");
        }
        return new ThumbImage(fullSizeUrl, thumbUrl);
    }

    private String uploadImage(byte[] data, String fileName) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("smfile", fileName, data, "image/jpg")
                .build();
        HttpRequest reqUpload = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", body.getContentType())
                .header("User-Agent",
                        "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://www.zbx1425.cn")
                .header("Authorization", "Basic " + apiToken)
                .POST(body.getBodyPublisher())
                .build();
        HttpResponse<String> respUpload = HTTP_CLIENT.send(reqUpload, HttpResponse.BodyHandlers.ofString());
        if (respUpload.statusCode() != 200)
            throw new IOException("Upload HTTP " + respUpload.statusCode() + "\n" + respUpload.body());
        JsonObject respObj = JsonParser.parseString(respUpload.body()).getAsJsonObject();
        if (!respObj.get("success").getAsBoolean()) {
            if (respObj.get("code").getAsString().equals("image_repeated")) {
                return respObj.get("images").getAsString();
            } else {
                throw new IOException("Upload Fail " + respUpload.body());
            }
        } else {
            return respObj.get("data").getAsJsonObject().get("url").getAsString();
        }
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", "smms");
        obj.addProperty("apiUrl", apiUrl);
        obj.addProperty("apiToken", apiToken);
        return obj;
    }
}
