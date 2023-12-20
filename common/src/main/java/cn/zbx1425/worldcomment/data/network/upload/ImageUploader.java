package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ThumbImage;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;

public abstract class ImageUploader {

    protected static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public abstract ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) throws IOException, InterruptedException;

    public static ImageUploader getUploader(ImageUploadConfig config) {
        return switch (config.service) {
            case "" -> NoopUploader.INSTANCE;
            case "imgloc" -> new ImglocUploader(config);
            case "smms" -> new SmmsUploader(config);
            case "lsky" -> new LskyUploader(config);
            default -> throw new IllegalStateException("Unknown service: " + config.service);
        };
    }

    private static class NoopUploader extends ImageUploader {

        protected static NoopUploader INSTANCE = new NoopUploader();

        @Override
        public ThumbImage uploadImage(byte[] imageBytes, CommentEntry comment) {
            return new ThumbImage("", "");
        }
    }
}
