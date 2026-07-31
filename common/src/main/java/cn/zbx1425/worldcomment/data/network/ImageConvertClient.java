package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.data.network.upload.ImageVariantConfig;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import dev.matrixlab.webp4j.WebPCodec;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class ImageConvertClient {

    public static byte[] toWebp(byte[] pngImageBytes, ImageVariantConfig.VariantSpec spec) {
        ByteBuffer offHeapPngData = OffHeapAllocator.allocate(pngImageBytes.length);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            offHeapPngData.put(pngImageBytes);
            offHeapPngData.rewind();
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            IntBuffer channels = memoryStack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(offHeapPngData, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Failed to load PNG image: " + STBImage.stbi_failure_reason());
            }
            pixels.rewind();
            try {
                int origW = width.get(0);
                int origH = height.get(0);
                int newWidth = (spec.maxWidth() > 0 && origW > spec.maxWidth()) ? spec.maxWidth() : origW;
                int newHeight = (newWidth == origW) ? origH : (int) (origH * (newWidth / (double) origW));

                ByteBuffer sourcePixels;
                boolean needFreeScaled = false;
                if (newWidth != origW) {
                    ByteBuffer scaledPixels = OffHeapAllocator.allocate(newWidth * newHeight * 4);
                    if (STBImageResize.stbir_resize_uint8_linear(pixels, origW, origH, 0,
                            scaledPixels, newWidth, newHeight, 0, 4) == null) {
                        OffHeapAllocator.free(scaledPixels);
                        throw new IllegalStateException("Failed to resize image");
                    }
                    sourcePixels = scaledPixels;
                    needFreeScaled = true;
                } else {
                    sourcePixels = pixels;
                }

                try {
                    byte[] rgbaBytes = new byte[newWidth * newHeight * 4];
                    sourcePixels.rewind();
                    sourcePixels.get(rgbaBytes);

                    BufferedImage img = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                    int[] argb = new int[newWidth * newHeight];
                    for (int i = 0; i < argb.length; i++) {
                        int r = rgbaBytes[i * 4] & 0xFF;
                        int g = rgbaBytes[i * 4 + 1] & 0xFF;
                        int b = rgbaBytes[i * 4 + 2] & 0xFF;
                        int a = rgbaBytes[i * 4 + 3] & 0xFF;
                        argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    img.setRGB(0, 0, newWidth, newHeight, argb, 0, newWidth);

                    try {
                        if (spec.lossless()) {
                            return WebPCodec.encodeLosslessImage(img);
                        } else {
                            return WebPCodec.encodeImage(img, spec.quality());
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("WebP encoding failed", e);
                    }
                } finally {
                    if (needFreeScaled) {
                        OffHeapAllocator.free(sourcePixels);
                    }
                }
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            OffHeapAllocator.free(offHeapPngData);
        }
    }

    public static byte[] webpToPng(byte[] webpData) {
        try {
            BufferedImage img = WebPCodec.decodeImage(webpData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(webpData.length * 2);
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert WebP to PNG", e);
        }
    }

    public static byte[] toJpeg(byte[] pngImageBytes) {
        ByteBuffer offHeapPngData = OffHeapAllocator.allocate(pngImageBytes.length);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            offHeapPngData.put(pngImageBytes);
            offHeapPngData.rewind();
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            IntBuffer channels = memoryStack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(offHeapPngData, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Failed to load PNG image: " + STBImage.stbi_failure_reason());
            }
            try {
                try (ByteArrayWriteCallback writeCallback = new ByteArrayWriteCallback(pngImageBytes.length)) {
                    if (STBImageWrite.stbi_write_jpg_to_func(writeCallback, 0, width.get(0), height.get(0), 4, pixels, 85) == 0) {
                        throw new IllegalStateException("Failed to write JPEG image: " + STBImage.stbi_failure_reason());
                    }
                    return writeCallback.array();
                }
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            OffHeapAllocator.free(offHeapPngData);
        }
    }

    public static byte[] toPng(byte[] jpegImageBytes) {
        ByteBuffer offHeapJpegData = OffHeapAllocator.allocate(jpegImageBytes.length);
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            offHeapJpegData.put(jpegImageBytes);
            offHeapJpegData.rewind();
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            IntBuffer channels = memoryStack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(offHeapJpegData, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Failed to load JPEG image: " + STBImage.stbi_failure_reason());
            }
            try {
                try (ByteArrayWriteCallback writeCallback = new ByteArrayWriteCallback(jpegImageBytes.length)) {
                    if (!STBImageWrite.stbi_write_png_to_func(writeCallback, 0, width.get(0), height.get(0), 4, pixels, width.get(0) * 4)) {
                        throw new IllegalStateException("Failed to write PNG image: " + STBImage.stbi_failure_reason());
                    }
                    return writeCallback.array();
                }
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            OffHeapAllocator.free(offHeapJpegData);
        }
    }

    private static class ByteArrayWriteCallback extends STBIWriteCallback {

        private final ByteArrayOutputStream buffer;
        private final WritableByteChannel channel;

        public ByteArrayWriteCallback(int capacity) {
            buffer = new ByteArrayOutputStream(capacity);
            channel = Channels.newChannel(buffer);
        }

        @Override
        public void invoke(long context, long data, int size) {
            try {
                channel.write(getData(data, size));
            } catch (IOException ignored) {

            }
        }

        public byte[] array() {
            return buffer.toByteArray();
        }
    }
}
