package cn.zbx1425.worldcomment.data.network.upload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

public class UrlTemplate {

    private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String transform(String template, long commentId, CommentAffinityInfo comment, ImageFilePurpose variant) {
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

            return switch (key) {
                case "id" -> String.format("%016x", commentId);
                case "variant" -> variant.fileTag();
                case ".variant" -> variant.dotFileTag();
                case "initiator" -> comment.initiator.toString();
                case "initiatorName" -> {
                    String sanitized = comment.initiatorName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    yield sanitized.isEmpty() ? "anonymous" : sanitized;
                }
                case "Y" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
                case "y" -> now.format(DateTimeFormatter.ofPattern("yy"));
                case "m" -> now.format(DateTimeFormatter.ofPattern("MM"));
                case "d" -> now.format(DateTimeFormatter.ofPattern("dd"));
                case "timestamp" -> Long.toString(System.currentTimeMillis() / 1000);
                case "uniqid" -> generateUniqid();
                default -> match.group(0);
            };
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
        long usec = (m % 1000) * 1000;
        return String.format("%08x%05x", sec, usec);
    }
}
