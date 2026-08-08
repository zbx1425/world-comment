package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.network.PacketPreSignRequestC2S;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class S3PreSignedUploader extends ImageUploader {

    public static final String DEFAULT_PATH_FORMAT = "{y}{m}/{d}/{id}-{initiatorName}{.variant}";

    public static class ServerConfig {
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
            this.pathFormat = config.has("pathFormat")
                    ? config.get("pathFormat").getAsString() : DEFAULT_PATH_FORMAT;
        }
    }

    private final ServerConfig serverConfig;

    public S3PreSignedUploader(String id, JsonObject serializedOrConfig) {
        super(id, "s3PreSigned", serializedOrConfig);
        if (serializedOrConfig.has("s3AccessKeyId")) {
            this.serverConfig = new ServerConfig(serializedOrConfig);
        } else {
            this.serverConfig = null;
        }
    }

    private static final Map<Long, CompletableFuture<PreSignResponse>> pendingPreSign = new HashMap<>();
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public CompletableFuture<UploadResult> uploadImage(byte[] imageData, String filename, CommentAffinityInfo info) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create("about:blank"))
                        .build();
                throw new UnsupportedOperationException("S3 upload must go through PreSign flow via Orchestrator");
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<PreSignResponse> requestPreSign(long jobId, CommentAffinityInfo info) {
        CompletableFuture<PreSignResponse> future = new CompletableFuture<PreSignResponse>()
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        synchronized (pendingPreSign) {
            CompletableFuture<PreSignResponse> existing = pendingPreSign.get(jobId);
            if (existing != null && !existing.isDone()) {
                throw new IllegalStateException("Another presign is in progress for this job.");
            }
            if (existing != null) {
                pendingPreSign.remove(jobId);
            }
            pendingPreSign.put(jobId, future);
        }
        PacketPreSignRequestC2S.ClientLogics.send(jobId, info, this);
        return future;
    }

    public CompletableFuture<List<UploadOutcome.Warning>> uploadToS3(String presignedUrl, byte[] webpData) {
        return CompletableFuture.supplyAsync(() ->
                ImageUploader.requestBuilder(URI.create(presignedUrl))
                        .header("Content-Type", "image/webp")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(webpData))
                        .build(), Main.IO_EXECUTOR)
                .thenCompose(request -> Main.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new CompletionException(new IOException(
                                "S3 upload failed: " + response.statusCode() + " " + response.body()));
                    }

                    List<UploadOutcome.Warning> warnings = new ArrayList<>();

                    // "S3ish" reverse proxy support
                    Optional<String> s3ishStatus = response.headers().firstValue("x-s3ish-status");
                    if (s3ishStatus.isPresent() && s3ishStatus.get().equals("rejected")) {
                        JsonObject errorJsonObj;
                        try {
                            errorJsonObj = JsonParser.parseString(response.body()).getAsJsonObject();
                        } catch (Exception ex) {
                            warnings.add(new ModerationWarning(ModerationWarning.Code.MODERATION_ERROR, ex.toString()));
                            return warnings;
                        }
                        switch (errorJsonObj.get("code").getAsString().toUpperCase(Locale.ROOT)) {
                            case "MODERATION_ERROR" -> warnings.add(new ModerationWarning(
                                    ModerationWarning.Code.MODERATION_ERROR,
                                    errorJsonObj.get("message").getAsString()));
                            case "MODERATION_REJECTED" -> warnings.add(new ModerationWarning(
                                    ModerationWarning.Code.MODERATION_REJECTED,
                                    errorJsonObj.get("message").getAsString()));
                        }
                    }
                    return warnings;
                });
    }

    public PreSignResponse performPreSign(long commentId, CommentAffinityInfo comment, ImageVariantConfig variantConfig) throws Exception {
        List<PreSignedSlot> slots = new ArrayList<>();

        String sourcePath = UrlTemplate.transformUpload(serverConfig.pathFormat, commentId, comment, ImageFilePurpose.SOURCE) + ".webp";
        slots.add(makeSlot(ImageFilePurpose.SOURCE, sourcePath));

        if (variantConfig.hasArchive() && !hasCdnTransform()) {
            String mediumPath = UrlTemplate.transformUpload(serverConfig.pathFormat, commentId, comment, ImageFilePurpose.MEDIUM) + ".webp";
            slots.add(makeSlot(ImageFilePurpose.MEDIUM, mediumPath));
        }
        if (variantConfig.hasThumbnail() && !hasCdnTransform()) {
            String thumbPath = UrlTemplate.transformUpload(serverConfig.pathFormat, commentId, comment, ImageFilePurpose.THUMBNAIL) + ".webp";
            slots.add(makeSlot(ImageFilePurpose.THUMBNAIL, thumbPath));
        }

        return new PreSignResponse(slots);
    }

    private PreSignedSlot makeSlot(ImageFilePurpose purpose, String objectKey) throws Exception {
        String uploadUrl = generatePreSignedUrl(
                "PUT", serverConfig.s3Endpoint, serverConfig.s3Bucket, objectKey,
                serverConfig.s3Region, serverConfig.s3AccessKeyId, serverConfig.s3SecretAccessKey, 900
        );
        String accessUrl = serverConfig.cdnBaseUrl + (serverConfig.cdnBaseUrl.endsWith("/") ? "" : "/") + objectKey;
        return new PreSignedSlot(purpose, uploadUrl, accessUrl);
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

    @Override
    public JsonObject serializeForClient() {
        JsonObject json = super.serializeForClient();
        return json;
    }

    // --- PreSign data structures ---

    public record PreSignedSlot(ImageFilePurpose purpose, String uploadUrl, String accessUrl) {}

    public record PreSignResponse(List<PreSignedSlot> slots) {}

    // --- S3 signing utilities ---

    private static String generatePreSignedUrl(String httpMethod, String endpoint, String bucketName,
            String objectKey, String region, String accessKey, String secretKey,
            long expirationSeconds) throws Exception {
        Instant now = Instant.now();
        DateTimeFormatter amzFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
        String amzDate = amzFormatter.format(now);
        DateTimeFormatter stampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
        String dateStamp = stampFormatter.format(now);

        String host;
        String baseUrl;
        if (endpoint != null && !endpoint.isEmpty()) {
            URI endpointUri = new URI(endpoint);
            String portlessHost = bucketName + "." + endpointUri.getHost();
            String portSegment = endpointUri.getPort() == -1 ? "" : ":" + endpointUri.getPort();
            host = portlessHost + portSegment;
            baseUrl = endpointUri.getScheme() + "://" + host;
        } else {
            host = bucketName + ".s3." + region + ".amazonaws.com";
            baseUrl = "https://" + host;
        }
        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";

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

        String algorithm = "AWS4-HMAC-SHA256";
        String hashedCanonicalRequest = toHex(sha256(canonicalRequest));
        String stringToSign = algorithm + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;

        byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, "s3");
        byte[] signatureBytes = hmacSha256(signingKey, stringToSign);
        String signature = toHex(signatureBytes);

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
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                    || ch == '_' || ch == '-' || ch == '~' || ch == '.') {
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
}
