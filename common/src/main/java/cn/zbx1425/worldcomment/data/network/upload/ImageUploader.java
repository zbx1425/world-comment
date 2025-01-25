package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.BuildConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class ImageUploader {

    public abstract CompletableFuture<ThumbImage> uploadImage(byte[] imageBytes, CommentEntry comment);

    public abstract JsonObject serialize();

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
            return new JsonObject();
        }
    }

    public static int IMAGE_MAX_WIDTH = 1920;
    public static int THUMBNAIL_MAX_WIDTH = 256;
}
