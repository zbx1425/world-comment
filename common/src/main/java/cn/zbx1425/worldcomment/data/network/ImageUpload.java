package cn.zbx1425.worldcomment.data.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageUpload {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static String apiUrlBase = "https://imgse.com";
    private static String loginSubject = "imgchr-teaconmc@ldiorstudio.cn";
    private static String loginCredential = "Sg5KRGj6h9n5HKKC";

    private static String sessionCookie;

    public static ThumbImage uploadImage(Path imagePath) throws IOException, InterruptedException {
        HttpRequest reqGetToken = HttpRequest.newBuilder(URI.create(apiUrlBase + "/login"))
                .header("Cookie", sessionCookie == null ? "" : sessionCookie)
                .GET()
                .build();
        HttpResponse<String> respGetToken = HTTP_CLIENT.send(reqGetToken, HttpResponse.BodyHandlers.ofString());
        if (respGetToken.statusCode() >= 400)
            throw new IOException("Auth Token HTTP " + respGetToken.statusCode() + "\n" + respGetToken.body());
        Pattern tokenPattern = Pattern.compile("auth_token = \"([a-z0-9]*)\"");
        Matcher tokenMatcher = tokenPattern.matcher(respGetToken.body());
        if (!tokenMatcher.find()) throw new IOException("Cannot locate Auth Token");
        String authToken = tokenMatcher.group(1);
        respGetToken.headers().firstValue("Set-Cookie").ifPresent(setCookie -> {
            sessionCookie = setCookie.split(";")[0];
        });
        if (sessionCookie == null) throw new IOException("Cannot obtain session cookie");

        HttpRequest reqLogin = HttpRequest.newBuilder(URI.create(apiUrlBase + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", sessionCookie)
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(Map.of(
                        "login-subject", loginSubject,
                        "password", loginCredential,
                        "auth_token", authToken
                ))))
                .build();
        HttpResponse<String> respLogin = HTTP_CLIENT.send(reqLogin, HttpResponse.BodyHandlers.ofString());
        if (respLogin.statusCode() >= 400)
            throw new IOException("Login HTTP " + respGetToken.statusCode() + "\n" + respLogin.body());

        MimeMultipartData body = MimeMultipartData.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .addFile("source", imagePath, "application/octet-stream")
                .addText("type", "file")
                .addText("action", "upload")
                .addText("timestamp", Long.toString(System.currentTimeMillis()))
                .addText("auth_token", authToken)
                .addText("nsfw", "0")
                .build();
        HttpRequest reqUpload = HttpRequest.newBuilder(URI.create(apiUrlBase + "/json"))
                .header("Content-Type", body.getContentType())
                .header("Cookie", sessionCookie)
                .POST(body.getBodyPublisher())
                .build();
        HttpResponse<String> respUpload = HTTP_CLIENT.send(reqUpload, HttpResponse.BodyHandlers.ofString());
        if (respUpload.statusCode() >= 400)
            throw new IOException("Upload HTTP " + respUpload.statusCode() + "\n" + respUpload.body());
        JsonObject respObj = JsonParser.parseString(respUpload.body()).getAsJsonObject();
        if (!respObj.has("success")) throw new IOException("Upload Fail " + respUpload.body());
        return new ThumbImage(
                respObj.get("image").getAsJsonObject().get("image").getAsJsonObject().get("url").getAsString(),
                respObj.get("image").getAsJsonObject().get("thumb").getAsJsonObject().get("url").getAsString()
        );
    }

    private static String encodeForm(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return result.toString();
    }
}
