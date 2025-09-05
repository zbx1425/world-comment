package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

public class UrlTemplate {

    private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String transform(String template, CommentAffinityInfo comment, String path) {
        LocalDateTime now = LocalDateTime.now();

        return PATTERN.matcher(template).replaceAll(match -> {
            String key = match.group(1);

            if (key.startsWith("str-random-")) {
                try {
                    int length = Integer.parseInt(key.substring("str-random-".length()));
                    return generateRandomString(length);
                } catch (NumberFormatException e) {
                    return match.group(0);
                }
            }

            switch (key) {
                case "initiator":
                    return comment.initiator.toString();
                case "initiatorName":
                    String sanitized = comment.initiatorName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    return sanitized.isEmpty() ? "anonymous" : sanitized;
                case "thumbWidth":
                    return Integer.toString(ImageUploader.THUMBNAIL_MAX_WIDTH);
                case "quality100":
                    return Integer.toString(ImageUploader.THUMBNAIL_QUALITY);
                case "quality1":
                    return String.format("%.2f", ImageUploader.THUMBNAIL_QUALITY / 100f);
                case "path":
                    return path.startsWith("/") ? path.substring(1) : path;
                case "Y":
                    return now.format(DateTimeFormatter.ofPattern("yyyy"));
                case "y":
                    return now.format(DateTimeFormatter.ofPattern("yy"));
                case "m":
                    return now.format(DateTimeFormatter.ofPattern("MM"));
                case "d":
                    return now.format(DateTimeFormatter.ofPattern("dd"));
                case "timestamp":
                    return Long.toString(System.currentTimeMillis() / 1000);
                case "filename":
                    try {
                        return Paths.get(path).getFileName().toString();
                    } catch (InvalidPathException | NullPointerException e) {
                        return "";
                    }
                case "uniqid":
                    return generateUniqid();
                default:
                    return match.group(0);
            }
        });
    }

    private static String generateRandomString(int length) {
        if (length <= 0) return "";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    private static String generateUniqid() {
        long m = System.currentTimeMillis();
        long sec = m / 1000;
        long usec = (m % 1000) * 1000; // Emulate microseconds
        return String.format("%08x%05x", sec, usec);
    }
}
