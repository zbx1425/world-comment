package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.mixin.NativeImageAccessor;
import cn.zbx1425.worldcomment.util.FrameTask;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.sounds.SoundEvent;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.function.Consumer;

public class Screenshot {

    public static boolean isGrabbing = false;

#if MC_VERSION >= "12106"
    public static void grabScreenshot(Consumer<byte[]> callback) {
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

    private static final SoundEvent shutterSoundEvent = #if MC_VERSION >= "11903" SoundEvent.createFixedRangeEvent #else new SoundEvent #endif (
            Main.id("shutter"), 16
    );

    public static void triggerCommentSend(boolean withPlacingDown) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || minecraft.screen instanceof ChatScreen) {
            boolean prevHideGui = minecraft.options.hideGui;
            applyClientConfigForScreenshot();
            // This is a workaround for the issue that the screenshot will be taken before CommentWorldRenderer is hidden
            FrameTask.enqueue(() -> {
                grabScreenshot(imageBytes -> minecraft.execute(() -> {
                    boolean onGround = #if MC_VERSION >= "12000" minecraft.player.onGround() #else minecraft.player.isOnGround() #endif;
                    boolean canSend = withPlacingDown || onGround;
                    if (minecraft.player != null && canSend) {
                        minecraft.player.playSound(shutterSoundEvent);
                        Minecraft.getInstance().setScreen(new CommentToolScreen(imageBytes, withPlacingDown));
                    }
                }));
                minecraft.options.hideGui = prevHideGui;
                MainClient.CLIENT_CONFIG.transientPreference.commentVisibilityMask = true;
                isGrabbing = false;
            }, 2);
        }
    }

    public static void applyClientConfigForScreenshot() {
        isGrabbing = true;
        Minecraft.getInstance().options.hideGui = !MainClient.CLIENT_CONFIG.transientPreference.screenshotIncludeGui;
        MainClient.CLIENT_CONFIG.transientPreference.commentVisibilityMask = MainClient.CLIENT_CONFIG.transientPreference.screenshotIncludeComments;
    }
}
