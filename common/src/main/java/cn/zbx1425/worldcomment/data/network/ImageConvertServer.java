package cn.zbx1425.worldcomment.data.network;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ImageConvertServer {

    public static byte[] toJpegScaled(byte[] pngImageBytes, int maxWidth) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(pngImageBytes));
        if (originalImage == null) {
            throw new IOException("Failed to read image");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int thumbWidth = Math.min(originalWidth, maxWidth);
        int thumbHeight = (int) ((float) originalHeight * thumbWidth / originalWidth);

        BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbImage.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, thumbWidth, thumbHeight);

            g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        JPEGImageWriteParam writeParam = new JPEGImageWriteParam(Locale.getDefault());
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(0.90f);
        writeParam.setOptimizeHuffmanTables(true);
        
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(thumbImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }
} 