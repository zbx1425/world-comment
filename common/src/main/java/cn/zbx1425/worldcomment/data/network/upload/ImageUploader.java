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
import java.util.concurrent.CompletableFuture;

public abstract class ImageUploader {

    public abstract CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment);

    public abstract JsonObject serialize();

    public JsonObject serializeForClient() {
        return serialize();
    }

    public static List<ImageUploader> parseUploaderList(List<JsonObject> configs) {
        List<ImageUploader> uploaders = new ArrayList<>();
        if (configs == null || configs.isEmpty()) {
            // 如果没有配置任何上传器，使用LocalStorageUploader作为默认
            uploaders.add(LocalStorageUploader.getInstance());
        } else {
            // 如果用户配置了上传器，就只使用配置的上传器
            for (JsonObject config : configs) {
                uploaders.add(getUploader(config));
            }
        }
        // 无论如何都在最后添加NoopUploader作为fallback
        uploaders.add(NoopUploader.INSTANCE);
        return uploaders;
    }

    public static ImageUploader getUploader(JsonObject config) {
        String service = config.has("service") ? config.get("service").getAsString() : "";
        return switch (service.toLowerCase()) {
            case "" -> NoopUploader.INSTANCE;
            case "local" -> LocalStorageUploader.deserialize(config);
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

        @Override
        public CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment) {
            Main.LOGGER.warn("No-op image uploader used for comment from {}. " +
                    "Either no uploader is configured or all uploads failed.", comment.initiator);
            return CompletableFuture.completedFuture(ThumbImage.NONE);
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("service", "");
            return obj;
        }
    }

    public static int IMAGE_MAX_WIDTH = 1920;
    public static int THUMBNAIL_MAX_WIDTH = 256;
    public static int THUMBNAIL_QUALITY = 85;
}
