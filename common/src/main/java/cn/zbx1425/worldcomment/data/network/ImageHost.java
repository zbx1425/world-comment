package cn.zbx1425.worldcomment.data.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ImageHost {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static String apiUrl = "https://storage.zbx1425.cn/img-lsky/api/v1/upload";
    private static String token = "1|FcYzmMmcCylQjcWkk4GdRvNUgGlbE5e7Eh752WOQ";

    public static ThumbImage uploadImage(Path imagePath) throws IOException, InterruptedException {
        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("file", imagePath, "application/octet-stream")
                .addText("strategy_id", "1")
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", body.getContentType())
                .header("Authorization", "Bearer " + token)
                .POST(body.getBodyPublisher())
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("HTTP response " + response.statusCode());
        JsonObject linkObj = JsonParser.parseString(response.body()).getAsJsonObject()
                .get("data").getAsJsonObject().get("links").getAsJsonObject();
        return new ThumbImage(linkObj.get("url").getAsString(), linkObj.get("thumbnail_url").getAsString());
    }
}
