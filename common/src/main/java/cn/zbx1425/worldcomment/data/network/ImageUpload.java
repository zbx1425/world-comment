package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.BuildConfig;
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

    private static String apiUrl = "https://smms.app/api/v2/upload";
    private static String apiToken = "ZpWPoCoqZ8B5TR0mmIF7k4XFxcoaLdom";

    public static ThumbImage uploadImage(Path imagePath, int thumbWidth) throws IOException, InterruptedException {
        BufferedImage fullSizeImage = ImageIO.read(imagePath.toFile());
        String fullSizeUrl = uploadImage(Files.readAllBytes(imagePath), imagePath.getFileName().toString());
        String thumbUrl;
        if (fullSizeImage.getWidth() < thumbWidth) {
            thumbUrl = fullSizeUrl;
        } else {
            BufferedImage thumbImage = new BufferedImage(thumbWidth,
                    (int)((float)fullSizeImage.getHeight() / fullSizeImage.getWidth() * thumbWidth),
                    fullSizeImage.getType());
            thumbImage.getGraphics().drawImage(
                    fullSizeImage.getScaledInstance(thumbWidth, thumbImage.getHeight(), Image.SCALE_DEFAULT),
                    0, 0, null);
            ByteArrayOutputStream oStream = new ByteArrayOutputStream(64 * 1024);
            ImageIO.write(thumbImage, "png", oStream);
            thumbUrl = uploadImage(oStream.toByteArray(),
                    FilenameUtils.removeExtension(imagePath.getFileName().toString()) + ".thumb.png");
        }
        return new ThumbImage(fullSizeUrl, thumbUrl);
    }

    public static String uploadImage(byte[] data, String fileName) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("smfile", fileName, data, "image/png")
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
}
