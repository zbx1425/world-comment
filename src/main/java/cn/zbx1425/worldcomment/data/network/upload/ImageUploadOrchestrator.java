package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.CommentImage;
import cn.zbx1425.worldcomment.data.network.ImageConvertClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImageUploadOrchestrator {

    public static CompletableFuture<UploadOutcome> upload(
            byte[] rawPngScreenshot,
            ImageUploader uploader,
            ImageVariantConfig variantConfig,
            CommentAffinityInfo info,
            long jobId
    ) {
        if (uploader instanceof LocalStorageUploader localUploader) {
            return uploadViaLocalStorage(rawPngScreenshot, localUploader, variantConfig, jobId);
        } else if (uploader instanceof S3PreSignedUploader s3Uploader) {
            return uploadViaS3PreSign(rawPngScreenshot, s3Uploader, variantConfig, info, jobId);
        } else {
            return uploadViaHttp(rawPngScreenshot, uploader, variantConfig, info, jobId);
        }
    }

    private static CompletableFuture<UploadOutcome> uploadViaLocalStorage(
            byte[] rawPng, LocalStorageUploader uploader, ImageVariantConfig variantConfig, long jobId) {
        return CompletableFuture.supplyAsync(() -> {
            ImageVariantConfig.VariantSpec sourceSpec = variantConfig.getSourceSpec();
            return ImageConvertClient.pngToWebp(rawPng, sourceSpec);
        }, Main.IO_EXECUTOR)
                .thenCompose(sourceWebp -> uploader.uploadForCommentImage(jobId, sourceWebp))
                .thenApply(UploadOutcome::success);
    }

    private static CompletableFuture<UploadOutcome> uploadViaS3PreSign(
            byte[] rawPng, S3PreSignedUploader s3Uploader,
            ImageVariantConfig variantConfig, CommentAffinityInfo info, long jobId) {
        return s3Uploader.requestPreSign(jobId, info)
                .thenCompose(preSignResponse -> {
                    CompletableFuture<?>[] uploadFutures = new CompletableFuture[preSignResponse.slots().size()];
                    String[] urls = new String[3]; // [source, medium, thumb]
                    List<UploadOutcome.Warning> allWarnings = Collections.synchronizedList(new ArrayList<>());

                    for (int i = 0; i < preSignResponse.slots().size(); i++) {
                        S3PreSignedUploader.PreSignedSlot slot = preSignResponse.slots().get(i);
                        ImageVariantConfig.VariantSpec spec = switch (slot.purpose()) {
                            case SOURCE -> variantConfig.getSourceSpec();
                            case MEDIUM -> variantConfig.detail();
                            case THUMBNAIL -> variantConfig.thumbnail();
                        };

                        uploadFutures[i] = CompletableFuture.supplyAsync(
                                () -> ImageConvertClient.pngToWebp(rawPng, spec), Main.IO_EXECUTOR
                        ).thenCompose(webpData -> s3Uploader.uploadToS3(slot.uploadUrl(), webpData))
                                .thenAccept(warnings -> {
                                    allWarnings.addAll(warnings);
                                    switch (slot.purpose()) {
                                        case SOURCE -> urls[0] = slot.accessUrl();
                                        case MEDIUM -> urls[1] = slot.accessUrl();
                                        case THUMBNAIL -> urls[2] = slot.accessUrl();
                                    }
                                });
                    }

                    return CompletableFuture.allOf(uploadFutures).thenApply(v ->
                            new UploadOutcome(
                                    new CommentImage(s3Uploader.id,
                                            urls[0] != null ? urls[0] : "",
                                            urls[1] != null ? urls[1] : "",
                                            urls[2] != null ? urls[2] : ""),
                                    List.copyOf(allWarnings)));
                });
    }

    private static CompletableFuture<UploadOutcome> uploadViaHttp(
            byte[] rawPng, ImageUploader uploader,
            ImageVariantConfig variantConfig, CommentAffinityInfo info, long jobId) {
        boolean A = variantConfig.hasArchive();
        boolean T = variantConfig.hasThumbnail();
        boolean C = uploader.hasCdnTransform();
        boolean N = !C && uploader.useNativeThumbnail();

        ImageVariantConfig.VariantSpec sourceSpec = variantConfig.getSourceSpec();
        String sourceFilename = uploader.resolveFilename(jobId, info, ImageFilePurpose.SOURCE);
        CompletableFuture<ImageUploader.UploadResult> sourceUpload = CompletableFuture.supplyAsync(
                () -> ImageConvertClient.pngToWebp(rawPng, sourceSpec), Main.IO_EXECUTOR
        ).thenCompose(webpData ->
                uploader.uploadImage(webpData, sourceFilename, info));

        return sourceUpload.thenCompose(sourceResult -> {
            String sourceUrl = sourceResult.url();
            String lastNativeThumb = sourceResult.nativeThumbnailUrl();

            CompletableFuture<String> detailUrlFuture;
            CompletableFuture<String> lastNativeThumbFuture;

            if (A && !C) {
                String mediumFilename = uploader.resolveFilename(jobId, info, ImageFilePurpose.MEDIUM);
                CompletableFuture<ImageUploader.UploadResult> detailUpload = CompletableFuture.supplyAsync(
                        () -> ImageConvertClient.pngToWebp(rawPng, variantConfig.detail()), Main.IO_EXECUTOR
                ).thenCompose(webpData ->
                        uploader.uploadImage(webpData, mediumFilename, info));
                detailUrlFuture = detailUpload.thenApply(ImageUploader.UploadResult::url);
                lastNativeThumbFuture = detailUpload.thenApply(ImageUploader.UploadResult::nativeThumbnailUrl);
            } else {
                detailUrlFuture = CompletableFuture.completedFuture("");
                lastNativeThumbFuture = CompletableFuture.completedFuture(lastNativeThumb);
            }

            return detailUrlFuture.thenCombine(lastNativeThumbFuture, (detailUrl, nativeThumb) -> {
                CompletableFuture<String> thumbUrlFuture;
                if (T && !C) {
                    if (N) {
                        thumbUrlFuture = CompletableFuture.completedFuture(nativeThumb);
                    } else {
                        String thumbFilename = uploader.resolveFilename(jobId, info, ImageFilePurpose.THUMBNAIL);
                        thumbUrlFuture = CompletableFuture.supplyAsync(
                                () -> ImageConvertClient.pngToWebp(rawPng, variantConfig.thumbnail()), Main.IO_EXECUTOR
                        ).thenCompose(webpData ->
                                uploader.uploadImage(webpData, thumbFilename, info)
                        ).thenApply(ImageUploader.UploadResult::url);
                    }
                } else {
                    thumbUrlFuture = CompletableFuture.completedFuture("");
                }
                return thumbUrlFuture.thenApply(thumbUrl ->
                        UploadOutcome.success(new CommentImage(uploader.id, sourceUrl, detailUrl, thumbUrl)));
            }).thenCompose(f -> f);
        });
    }
}
