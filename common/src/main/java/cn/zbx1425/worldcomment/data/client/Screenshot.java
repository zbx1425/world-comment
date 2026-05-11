package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.mixin.NativeImageAccessor;
import cn.zbx1425.worldcomment.util.FrameTask;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.function.Consumer;

public class Screenshot {

    public static boolean isGrabbing = false;

#if MC_VERSION >= "12106"
    public static void grabScreenshotInternal(Consumer<byte[]> callback) {
        RenderTarget frameBuf = Minecraft.getInstance().getMainRenderTarget();
        net.minecraft.client.Screenshot.takeScreenshot(frameBuf, nativeImage -> {
            try (nativeImage) {
                ByteArrayOutputStream byteArraySink = new ByteArrayOutputStream();
                if (!((NativeImageAccessor)(Object)nativeImage).invokeWriteToChannel(Channels.newChannel(byteArraySink))) {
                    throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
                }
                callback.accept(byteArraySink.toByteArray());
            } catch (IOException ex) {
                Main.LOGGER.error("Failed to save screenshot", ex);
            }
        });
    }
#else
    public static void grabScreenshot(Consumer<byte[]> callback) {
        RenderTarget frameBuf = Minecraft.getInstance().getMainRenderTarget();
        NativeImage fullSizeImage = new NativeImage(frameBuf.width, frameBuf.height, false);
        try (fullSizeImage) {
            RenderSystem.bindTexture(frameBuf.getColorTextureId());
            fullSizeImage.downloadTexture(0, true);
            fullSizeImage.flipY();
            callback.accept(fullSizeImage.asByteArray());
        } catch (IOException ex) {
            Main.LOGGER.error("Failed to save screenshot", ex);
        }
    }
#endif

    public static void grabScreenshot(Consumer<byte[]> callback) {
        if (isGrabbing) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean prevHideGui = minecraft.options.hideGui;
        Screenshot.applyClientConfigForScreenshot();
        // This is a workaround for the issue that the screenshot will be taken before CommentWorldRenderer is hidden
        FrameTask.enqueue(() -> {
            Screenshot.grabScreenshotInternal(imageBytes -> minecraft.execute(() -> {
                callback.accept(imageBytes);
            }));
            minecraft.options.hideGui = prevHideGui;
            MainClient.CLIENT_CONFIG.transientPreference.commentVisibilityMask = true;
            Screenshot.isGrabbing = false;
        }, 2);
    }

    public static File getAvailableFile() {
        File screenShotDirectory = new File(Minecraft.getInstance().gameDirectory,"screenshots");
        String s = "WorldComment-" + Util.getFilenameFormattedDateTime();
        int i = 1;
        File file1;
        while ((file1 = new File(screenShotDirectory, s + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
            ++i;
        }
        return file1;
    }

    public static void applyClientConfigForScreenshot() {
        isGrabbing = true;
        Minecraft.getInstance().options.hideGui = !MainClient.CLIENT_CONFIG.transientPreference.screenshotIncludeGui;
        MainClient.CLIENT_CONFIG.transientPreference.commentVisibilityMask = MainClient.CLIENT_CONFIG.transientPreference.screenshotIncludeComments;
    }
}
