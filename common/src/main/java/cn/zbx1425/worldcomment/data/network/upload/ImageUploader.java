package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.google.gson.JsonObject;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.Locale;

public abstract class ImageUploader {

    protected static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public abstract ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) throws IOException, InterruptedException;

    public static ImageUploader getUploader(JsonObject config) {
        String service = config.has("service") ? config.get("service").getAsString() : "";
        return switch (service.toLowerCase(Locale.ROOT)) {
            case "" -> NoopUploader.INSTANCE;
            case "imgloc" -> new ImglocUploader(config);
            case "smms" -> new SmmsUploader(config);
            case "lsky" -> new LskyUploader(config);
            default -> throw new IllegalStateException("Unknown service: " + service);
        };
    }

    public abstract JsonObject serialize();

    public static class NoopUploader extends ImageUploader {

        public static NoopUploader INSTANCE = new NoopUploader();

        @Override
        public ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) {
            Main.LOGGER.warn("No-op image uploader used for comment from {}. " +
                    "Either no uploader is configured or all uploads failed.", comment.initiator);
            return new ThumbImage("", "");
        }

        @Override
        public JsonObject serialize() {
            return new JsonObject();
        }
    }

    public static int IMAGE_MAX_WIDTH = 1920;
    public static int THUMBNAIL_MAX_WIDTH = 256;
}
