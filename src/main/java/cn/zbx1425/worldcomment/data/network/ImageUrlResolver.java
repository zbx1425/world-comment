package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.upload.CdnTransformConfig;
import cn.zbx1425.worldcomment.data.network.upload.ImageFilePurpose;
import cn.zbx1425.worldcomment.data.network.upload.ImageVariantConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

public class ImageUrlResolver {

    public enum ImageUsagePurpose { ARCHIVE, DETAIL, THUMBNAIL }

    public static @NonNull String resolve(
            @NonNull CommentImage image,
            ImageUrlResolver.@NonNull ImageUsagePurpose imageUsagePurpose,
            @NonNull ImageVariantConfig variantConfig,
            @Nullable Map<String, CdnTransformConfig> uploaderCdnConfigs
    ) {
        if (image == CommentImage.NONE || image.sourceUrl.isEmpty()) {
            return "";
        }

        if (imageUsagePurpose == ImageUsagePurpose.ARCHIVE) {
            return image.sourceUrl;
        }

        String stored = switch (imageUsagePurpose) {
            case DETAIL -> image.mediumUrl;
            case THUMBNAIL -> image.thumbUrl;
            default -> "";
        };
        if (!stored.isEmpty()) {
            return stored;
        }

        CdnTransformConfig cdn = null;
        if (uploaderCdnConfigs != null && !image.uploaderId.isEmpty()) {
            cdn = uploaderCdnConfigs.get(image.uploaderId);
        }
        boolean cdnEnabled = cdn != null && cdn.isEnabled();

        if (cdnEnabled) {
            try {
                String cdnResult = null;
                URI sourceUrl = URI.create(image.sourceUrl);
                if (imageUsagePurpose == ImageUsagePurpose.THUMBNAIL) {
                    ImageVariantConfig.VariantSpec spec = variantConfig.hasThumbnail()
                            ? variantConfig.thumbnail() : variantConfig.detail();
                    ImageFilePurpose fileThatMayExist = variantConfig.hasThumbnail()
                            ? ImageFilePurpose.THUMBNAIL
                            : (variantConfig.hasArchive()
                                ? ImageFilePurpose.MEDIUM
                                : ImageFilePurpose.SOURCE);
                    cdnResult = cdn.apply(sourceUrl, fileThatMayExist, spec);
                }
                if (imageUsagePurpose == ImageUsagePurpose.DETAIL && variantConfig.hasArchive()) {
                    cdnResult = cdn.apply(sourceUrl, ImageFilePurpose.MEDIUM, variantConfig.detail());
                }
                // !hasArchive + DETAIL: source is already detail quality, skip CDN

                if (cdnResult != null && !cdnResult.isEmpty()) {
                    try {
                        return sourceUrl.resolve(cdnResult).toString();
                    } catch (IllegalArgumentException e) {
                        Main.LOGGER.warn("Exception when parsing cdnImageTransform result", e);
                        return cdnResult;
                    }
                }
            } catch (IllegalArgumentException e) {
                Main.LOGGER.warn("Exception when parsing image.sourceUrl", e);
            }
        }

        // Fallback: pick the best available URL
        if (imageUsagePurpose == ImageUsagePurpose.THUMBNAIL) {
            if (!image.mediumUrl.isEmpty()) return image.mediumUrl;
        }
        return image.sourceUrl;
    }
}
