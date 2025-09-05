package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageConvertClient;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.network.PacketImageUploadC2S;
import cn.zbx1425.worldcomment.network.PacketPreSignRequestC2S;
import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class S3PreSignedUploader extends ImageUploader {

    private static class ServerConfig {
        public final String s3Endpoint;
        public final String s3Bucket;
        public final String s3Region;
        public final String s3AccessKeyId;
        public final String s3SecretAccessKey;
        public final String cdnBaseUrl;
        public final String pathFormat;

        public ServerConfig(JsonObject config) {
            this.s3Endpoint = config.has("s3Endpoint") ? config.get("s3Endpoint").getAsString() : "";
            this.s3Bucket = config.get("s3Bucket").getAsString();
            this.s3Region = config.get("s3Region").getAsString();
            this.s3AccessKeyId = config.get("s3AccessKeyId").getAsString();
            this.s3SecretAccessKey = config.get("s3SecretAccessKey").getAsString();
            this.cdnBaseUrl = config.get("cdnBaseUrl").getAsString();
            this.pathFormat = config.get("pathFormat").getAsString();
        }
    }

    private final ServerConfig serverConfig;
    private final String cdnImageTransform;

    public S3PreSignedUploader(JsonObject serializedOrConfig) {
        super("s3PreSigned", serializedOrConfig);
        if (serializedOrConfig.has("s3AccessKeyId")) {
            this.serverConfig = new ServerConfig(serializedOrConfig);
        } else {
            this.serverConfig = null;
        }
        this.cdnImageTransform = serializedOrConfig.has("cdnImageTransform") ? serializedOrConfig.get("cdnImageTransform").getAsString() : null;
    }

    private static final Map<Long, CompletableFuture<PreSignResponse>> pendingPreSign = new HashMap<>();
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
        CompletableFuture<PreSignResponse> future = new CompletableFuture<PreSignResponse>().orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        synchronized (pendingPreSign) {
            CompletableFuture<PreSignResponse> existing = pendingPreSign.get(comment.id);
            if (existing != null && !existing.isDone()) {
                throw new IllegalStateException("Another upload is in progress for this comment.");
            }
            if (existing != null) {
                pendingPreSign.remove(comment.id);
            }
            pendingPreSign.put(comment.id, future);
        }
        PacketPreSignRequestC2S.ClientLogics.send(comment.id, new CommentAffinityInfo(comment), this);
        return future
                .thenCompose(preSign -> {
                    if (cdnImageTransform == null) {
                        CompletableFuture<Void> fullSizeUpload = uploadToS3(preSign.upload.url, imageBytes, IMAGE_MAX_WIDTH);
                        CompletableFuture<Void> thumbUpload = uploadToS3(preSign.upload.thumbUrl, imageBytes, THUMBNAIL_MAX_WIDTH);
                        return CompletableFuture.allOf(fullSizeUpload, thumbUpload)
                                .thenApply(v -> new ThumbImage(preSign.access.url, preSign.access.thumbUrl));
                    } else {
                        return uploadToS3(preSign.upload.url, imageBytes, IMAGE_MAX_WIDTH)
                                .thenApply(v -> {
                                    String publicUrl = preSign.access.url;
                                    String thumbUrl = transformUrl(comment, publicUrl);
                                    return new ThumbImage(publicUrl, thumbUrl);
                                });
                    }
                });
    }

    public PreSignResponse performPreSign(CommentAffinityInfo comment) throws Exception {
        String targetPathBase = UrlTemplate.transform(serverConfig.pathFormat, comment, "");
        String targetPathFull = targetPathBase + ".jpg";
        String targetPathThumb = targetPathBase + ".thumb.jpg";

        String preSignedUrlFull = generatePreSignedUrl(
                "PUT", serverConfig.s3Endpoint, serverConfig.s3Bucket, targetPathFull, serverConfig.s3Region,
                serverConfig.s3AccessKeyId, serverConfig.s3SecretAccessKey, 900 // 15 minutes
        );
        String preSignedUrlThumb = generatePreSignedUrl(
                "PUT", serverConfig.s3Endpoint, serverConfig.s3Bucket, targetPathThumb, serverConfig.s3Region,
                serverConfig.s3AccessKeyId, serverConfig.s3SecretAccessKey, 900 // 15 minutes
        );

        return new PreSignResponse(
                new ThumbImage(preSignedUrlFull, preSignedUrlThumb),
                new ThumbImage(serverConfig.cdnBaseUrl + "/" + targetPathFull, serverConfig.cdnBaseUrl + "/" + targetPathThumb)
        );
    }

    public static void completePreSign(long jobId, PreSignResponse response) {
        CompletableFuture<PreSignResponse> future;
        synchronized (pendingPreSign) {
            future = pendingPreSign.remove(jobId);
        }
        if (future != null) {
            future.complete(response);
        }
    }

    public static void completePreSignExceptionally(long jobId, Throwable ex) {
        CompletableFuture<PreSignResponse> future;
        synchronized (pendingPreSign) {
            future = pendingPreSign.remove(jobId);
        }
        if (future != null) {
            future.completeExceptionally(ex);
        }
    }

    private static String generatePreSignedUrl(String httpMethod, String endpoint, String bucketName, String objectKey, String region, String accessKey, String secretKey, long expirationSeconds) throws Exception {
        // 1. Create timestamp and datestamp for the signature
        Instant now = Instant.now();
        DateTimeFormatter amzFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
        String amzDate = amzFormatter.format(now);
        DateTimeFormatter stampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
        String dateStamp = stampFormatter.format(now);

        String host;
        String baseUrl;
        if (endpoint != null && !endpoint.isEmpty()) {
            URI endpointUri = new URI(endpoint);
            host = endpointUri.getHost();
            baseUrl = endpointUri.getScheme() + "://" + host;
        } else {
            host = bucketName + ".s3." + region + ".amazonaws.com";
            baseUrl = "https://" + host;
        }
        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";

        // 2. Create canonical query string
        Map<String, String> queryParams = new TreeMap<>();
        queryParams.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
        queryParams.put("X-Amz-Credential", accessKey + "/" + credentialScope);
        queryParams.put("X-Amz-Date", amzDate);
        queryParams.put("X-Amz-Expires", String.valueOf(expirationSeconds));
        queryParams.put("X-Amz-SignedHeaders", "host");

        StringBuilder canonicalQueryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!canonicalQueryString.isEmpty()) {
                canonicalQueryString.append("&");
            }
            canonicalQueryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        // 3. Create canonical request
        String canonicalURI = "/" + uriEncode(objectKey, false);
        String hashedPayload = "UNSIGNED-PAYLOAD";
        String canonicalHeaders = "host:" + host + "\n";
        String signedHeaders = "host";

        String canonicalRequest = httpMethod + "\n"
                + canonicalURI + "\n"
                + canonicalQueryString + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedPayload;

        // 4. Create string to sign
        String algorithm = "AWS4-HMAC-SHA256";
        String hashedCanonicalRequest = toHex(sha256(canonicalRequest));
        String stringToSign = algorithm + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;

        // 5. Derive signing key
        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, "s3");

        // 6. Calculate signature
        byte[] signatureBytes = hmacSha256(signingKey, stringToSign);
        String signature = toHex(signatureBytes);

        // 7. Assemble the final URL
        return baseUrl + canonicalURI + "?" + canonicalQueryString + "&X-Amz-Signature=" + signature;
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private static String uriEncode(CharSequence input, boolean encodeSlash) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
                result.append(ch);
            } else if (ch == '/') {
                result.append(encodeSlash ? "%2F" : ch);
            } else {
                result.append(String.format("%%%02X", (int) ch).toUpperCase());
            }
        }
        return result.toString();
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return md.digest();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private CompletableFuture<Void> uploadToS3(String presignedUrl, byte[] imageBytes, int maxWidth) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] scaledImage = ImageConvertClient.toJpegScaled(imageBytes, maxWidth);
                return HttpRequest.newBuilder(URI.create(presignedUrl))
                        .header("Content-Type", "image/jpeg")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(scaledImage))
                        .build();
            } catch (IllegalStateException e) {
                throw new CompletionException(e);
            }
        }, Main.IO_EXECUTOR)
                .thenCompose(request -> Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding()))
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("S3 upload failed: " + response.statusCode()));
                    }
                });
    }

    private String transformUrl(CommentEntry comment, String originalUrl) {
        try {
            URI uri = URI.create(originalUrl);
            String path = uri.getPath();
            return originalUrl.replace(path, UrlTemplate.transform(cdnImageTransform, new CommentAffinityInfo(comment), path));
        } catch (Exception e) {
            Main.LOGGER.error("Error transforming thumbnail URL", e);
            return originalUrl;
        }
    }

    @Override
    public JsonObject serializeForClient() {
        JsonObject json = super.serializeForClient();
        if (cdnImageTransform != null) json.addProperty("cdnImageTransform", cdnImageTransform);
        return json;
    }

    public record PreSignResponse(ThumbImage upload, ThumbImage access) {}
}
