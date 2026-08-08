package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class ImageUploader {

    public static final String DEFAULT_FILENAME_FORMAT = "{id}-{initiatorName}{.variant}";

    public final @NonNull String id;
    public final @NonNull String serviceName;
    public final @NonNull CdnTransformConfig cdnConfig;
    public final @NonNull String filenameFormat;
    public final boolean ignoreNativeThumbnail;

    public ImageUploader(String id, String serviceName, JsonObject config) {
        this.id = id;
        this.serviceName = serviceName;
        if (config != null) {
            this.cdnConfig = CdnTransformConfig.fromJson(config);
            this.ignoreNativeThumbnail = config.has("ignoreNativeThumbnail")
                    && config.get("ignoreNativeThumbnail").getAsBoolean();
            this.filenameFormat = config.has("filenameFormat")
                    ? config.get("filenameFormat").getAsString() : DEFAULT_FILENAME_FORMAT;
        } else {
            this.cdnConfig = new CdnTransformConfig("");
            this.ignoreNativeThumbnail = false;
            this.filenameFormat = DEFAULT_FILENAME_FORMAT;
        }
    }

    public String resolveFilename(long commentId, CommentAffinityInfo info, ImageFilePurpose variant) {
        return UrlTemplate.transformUpload(filenameFormat, commentId, info, variant) + ".webp";
    }

    public abstract CompletableFuture<UploadResult> uploadImage(
            byte[] imageData, String filename, CommentAffinityInfo info);

    public boolean hasNativeThumbnail() {
        return false;
    }

    public boolean hasCdnTransform() {
        return cdnConfig.isEnabled();
    }

    public boolean useNativeThumbnail() {
        return hasNativeThumbnail() && !ignoreNativeThumbnail;
    }

    public JsonObject serializeForClient() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", serviceName);
        obj.addProperty("id", id);
        if (cdnConfig.isEnabled()) {
            obj.addProperty("cdnImageTransform", cdnConfig.template());
        }
        if (ignoreNativeThumbnail) {
            obj.addProperty("ignoreNativeThumbnail", true);
        }
        return obj;
    }

    public static List<ImageUploader> parseUploaderList(List<JsonObject> configs) {
        List<ImageUploader> uploaders = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            uploaders.add(LocalStorageUploader.getInstance());
        } else {
            for (int i = 0; i < configs.size(); i++) {
                JsonObject config = configs.get(i);
                String id = resolveId(config, i);
                uploaders.add(getUploader(id, config));
            }
        }
        uploaders.add(NoopUploader.INSTANCE);
        return uploaders;
    }

    private static String resolveId(JsonObject config, int index) {
        if (config.has("id")) {
            return config.get("id").getAsString();
        }
        String service = config.has("service") ? config.get("service").getAsString() : "unknown";
        return service + "-" + index;
    }

    public static ImageUploader getUploader(String id, JsonObject config) {
        String service = config.has("service") ? config.get("service").getAsString() : "";
        return switch (service.toLowerCase()) {
            case "" -> NoopUploader.INSTANCE;
            case "local" -> LocalStorageUploader.getInstance();
            case "imgloc" -> new ImglocUploader(id, config);
            case "smms" -> new SmmsUploader(id, config);
            case "lsky" -> new LskyUploader(id, config);
            case "s3presigned" -> new S3PreSignedUploader(id, config);
            default -> throw new IllegalStateException("Unknown service: " + service);
        };
    }

    public static HttpRequest.Builder requestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("User-Agent",
                        "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://github.com/zbx1425")
                .header("X-Minecraft-Username", Minecraft.getInstance().getUser().getName())
                .header("X-Minecraft-UUID", Minecraft.getInstance().getUser().getProfileId().toString());
    }

    public static class NoopUploader extends ImageUploader {

        public static NoopUploader INSTANCE = new NoopUploader();

        public NoopUploader() {
            super("noop", "", null);
        }

        @Override
        public CompletableFuture<UploadResult> uploadImage(byte[] imageData, String filename, CommentAffinityInfo info) {
            Main.LOGGER.warn("No-op image uploader used. " +
                    "Either no uploader is configured or all uploads failed.");
            return CompletableFuture.completedFuture(new UploadResult(""));
        }
    }

    public record UploadResult(@NonNull String url, @NonNull String nativeThumbnailUrl) {
        public UploadResult(@NonNull String url) {
            this(url, "");
        }
    }
}
