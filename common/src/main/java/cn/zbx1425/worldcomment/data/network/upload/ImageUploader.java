package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ImageUploader {

    public final String serviceName;
    public final UUID instanceId;

    public ImageUploader(String serviceName, JsonObject serializedOrConfig) {
        this.serviceName = serviceName;
        if (serializedOrConfig == null || !serializedOrConfig.has("uploaderId")) {
            this.instanceId = UUID.randomUUID();
        } else {
            this.instanceId = UUID.fromString(serializedOrConfig.get("uploaderId").getAsString());
        }
    }

    public abstract CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment);

    public JsonObject serializeForClient() {
        JsonObject obj = new JsonObject();
        obj.addProperty("service", serviceName);
        obj.addProperty("instanceId", instanceId.toString());
        return obj;
    }

    public static List<ImageUploader> parseUploaderList(List<JsonObject> configs) {
        List<ImageUploader> uploaders = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            uploaders.add(LocalStorageUploader.getInstance());
        } else {
            for (JsonObject config : configs) {
                uploaders.add(getUploader(config));
            }
        }
        uploaders.add(NoopUploader.INSTANCE);
        return uploaders;
    }

    public static ImageUploader getUploader(JsonObject config) {
        String service = config.has("service") ? config.get("service").getAsString() : "";
        return switch (service.toLowerCase()) {
            case "" -> NoopUploader.INSTANCE;
            case "local" -> LocalStorageUploader.getInstance();
            case "imgloc" -> new ImglocUploader(config);
            case "smms" -> new SmmsUploader(config);
            case "lsky" -> new LskyUploader(config);
            case "s3presigned" -> new S3PreSignedUploader(config);
            default -> throw new IllegalStateException("Unknown service: " + service);
        };
    }

    public static HttpRequest.Builder requestBuilder(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("User-Agent",
                        "Mozilla/5.0 WorldComment/" + BuildConfig.MOD_VERSION + " +https://www.zbx1425.cn")
                .header("X-Minecraft-Username", Minecraft.getInstance().getUser().getName())
                .header("X-Minecraft-UUID", Minecraft.getInstance().getUser().getProfileId().toString())
        ;
    }

    public static class NoopUploader extends ImageUploader {

        public static NoopUploader INSTANCE = new NoopUploader();

        public NoopUploader() {
            super("", null);
        }

        @Override
        public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
            Main.LOGGER.warn("No-op image uploader used for comment from {}. " +
                    "Either no uploader is configured or all uploads failed.", comment.initiator);
            return CompletableFuture.completedFuture(ThumbImage.NONE);
        }

        @Override
        public JsonObject serializeForClient() {
            return super.serializeForClient();
        }
    }

    public static int IMAGE_MAX_WIDTH = 1920;
    public static int THUMBNAIL_MAX_WIDTH = 256;
    public static int THUMBNAIL_QUALITY = 85;
}
