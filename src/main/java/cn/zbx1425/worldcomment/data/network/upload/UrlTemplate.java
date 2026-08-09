package cn.zbx1425.worldcomment.data.network.upload;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class UrlTemplate {

    private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static String transformUpload(String template, long commentId, CommentAffinityInfo comment, ImageFilePurpose variant) {
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
                case "ID" -> String.format("%016X", commentId);
                case "variant" -> variant.fileTag();
                case ".variant" -> variant.dotFileTag();
                case "initiator" -> comment.initiator.toString();
                case "initiatorName" -> {
                    String sanitized = comment.initiatorName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    yield sanitized.isEmpty() ? "anonymous" : sanitized;
                }
                case "initiatorname" -> {
                    String sanitized = comment.initiatorName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);
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

    public static String transformCdn(String template, URI sourceUrl, ImageFilePurpose variant, ImageVariantConfig.VariantSpec variantSpec) {
        String path = sourceUrl.getPath();
        String pathNoLeadingSlash = path.startsWith("/") ? path.substring(1) : path;

        int lastSlashIdx = pathNoLeadingSlash.lastIndexOf('/');
        String dir = lastSlashIdx >= 0 ? pathNoLeadingSlash.substring(0, lastSlashIdx) : "";
        String fileNameExt = lastSlashIdx >= 0 ? pathNoLeadingSlash.substring(lastSlashIdx + 1) : pathNoLeadingSlash;

        int lastDotIdx = fileNameExt.lastIndexOf('.');
        String fileName;
        String dotExt;
        String ext;
        if (lastDotIdx > 0) {
            fileName = fileNameExt.substring(0, lastDotIdx);
            dotExt = fileNameExt.substring(lastDotIdx);          // 包含点
            ext = fileNameExt.substring(lastDotIdx + 1);         // 不含点
        } else {
            fileName = fileNameExt;
            dotExt = "";
            ext = "";
        }

        return PATTERN.matcher(template).replaceAll(match -> {
            String key = match.group(1);

            return switch (key) {
                case "path" -> pathNoLeadingSlash;
                case "dir" -> dir;
                case "fileName.ext" -> fileNameExt;
                case "fileName" -> fileName;
                case ".ext" -> dotExt;
                case "ext" -> ext;
                case "variant" -> variant.fileTag();
                case ".variant" -> variant.dotFileTag();
                case "width" -> Integer.toString(variantSpec.maxWidth());
                case "quality" -> Integer.toString(variantSpec.quality());
                case "0.quality" -> String.format("%.2f", variantSpec.quality() / 100f);
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
