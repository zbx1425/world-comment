package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.data.network.upload.ImageVariantConfig;
import dev.matrixlab.webp4j.WebPCodec;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageConvertServer {

    public static byte[] anyToWebp(byte[] sourceImageData, ImageVariantConfig.VariantSpec spec) throws IOException {
        BufferedImage originalImage = decodeImage(sourceImageData);
        if (originalImage == null) {
            throw new IOException("Failed to read image");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int targetWidth = (spec.maxWidth() > 0 && originalWidth > spec.maxWidth()) ? spec.maxWidth() : originalWidth;
        int targetHeight = (targetWidth == originalWidth) ? originalHeight
                : (int) ((float) originalHeight * targetWidth / originalWidth);

        BufferedImage targetImage;
        if (targetWidth != originalWidth) {
            targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = targetImage.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                g2d.dispose();
            }
        } else {
            targetImage = originalImage;
        }

        try {
            if (spec.lossless()) {
                return WebPCodec.encodeLosslessImage(targetImage);
            } else {
                return WebPCodec.encodeImage(targetImage, spec.quality());
            }
        } catch (Exception e) {
            throw new IOException("WebP encoding failed", e);
        }
    }

    private static BufferedImage decodeImage(byte[] data) throws IOException {
        if (isWebpData(data)) {
            try {
                return WebPCodec.decodeImage(data);
            } catch (Exception e) {
                throw new IOException("Failed to decode WebP image", e);
            }
        }
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    private static boolean isWebpData(byte[] data) {
        return data.length > 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }
}
