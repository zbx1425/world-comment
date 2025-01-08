package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class ImageConvert {

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
                    if (STBImageWrite.stbi_write_jpg_to_func(writeCallback, 0, width.get(0), height.get(0), 4, pixels, 100) == 0) {
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

    public static byte[] toJpegScaled(byte[] pngImageBytes, int maxWidth) {
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
                int newWidth = Math.min(width.get(0), maxWidth);
                int newHeight = (int) (height.get(0) * (newWidth / (double) width.get(0)));
                ByteBuffer scaledPixels = OffHeapAllocator.allocate(newWidth * newHeight * 4);
                try {
                    if (!STBImageResize.stbir_resize_uint8(pixels, width.get(0), height.get(0), 0, scaledPixels, newWidth, newHeight, 0, 4)) {
                        throw new IllegalStateException("Failed to resize image: " + STBImage.stbi_failure_reason());
                    }
                    try (ByteArrayWriteCallback writeCallback = new ByteArrayWriteCallback(pngImageBytes.length)) {
                        if (STBImageWrite.stbi_write_jpg_to_func(writeCallback, 0, newWidth, newHeight, 4, scaledPixels, 100) == 0) {
                            throw new IllegalStateException("Failed to write JPEG image: " + STBImage.stbi_failure_reason());
                        }
                        return writeCallback.array();
                    }
                } finally {
                    OffHeapAllocator.free(scaledPixels);
                }
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            OffHeapAllocator.free(offHeapPngData);
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
