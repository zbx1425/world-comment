package cn.zbx1425.worldcomment.data.network.uplink;

import cn.zbx1425.worldcomment.Main;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UplinkRequest {

    public final String url;
    public final String method;
    public final JsonObject payload;
    public final Consumer<JsonObject> callback;

    public UplinkRequest(String url, JsonObject payload) {
        this(url, "POST", payload, null);
    }

    public UplinkRequest(String url, String method, JsonObject payload, Consumer<JsonObject> callback) {
        this.url = url;
        this.method = method;
        this.payload = payload;
        this.callback = callback;
    }

    public void sendBlocking() {
        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setRequestMethod(method);
            if (payload != null) {
                JsonObject objectToSend = payload.deepCopy();
                objectToSend.addProperty("requestTimestamp", Instant.now().getEpochSecond());
                byte[] postDataBytes = objectToSend.toString().getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

                if (!Main.SERVER_CONFIG.uplinkAuthKey.value.isEmpty()) {
                    byte[] signatureBytes = Hashing
                            .hmacSha1(Main.SERVER_CONFIG.uplinkAuthKey.value.getBytes(StandardCharsets.UTF_8))
                            .hashBytes(postDataBytes).asBytes();
                    conn.setRequestProperty("Authorization", "NEX-HMAC-SHA1 Signature=" + Base64.encodeBase64String(signatureBytes));
                }

                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
            }

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0;) sb.append((char)c);
            Main.LOGGER.info(sb.toString());
            if (callback != null) {
                callback.accept(JsonParser.parseString(sb.toString()).getAsJsonObject());
            }
        } catch (Exception ex) {
            Main.LOGGER.warn("Failed sending uplink request: ", ex);
        }
    }

    public void sendAsync() {
        executor.submit(this::sendBlocking);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
}
