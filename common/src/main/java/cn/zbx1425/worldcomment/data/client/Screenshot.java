package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class Screenshot {

    public static boolean isGrabbing = false;

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

    public static boolean handleKeyF2() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
        if (item == null) return false;
        if (CommentToolItem.getUploadJobId(item) != null) return false;

        if (minecraft.screen == null) {
            boolean prevHideGui = minecraft.options.hideGui;
            applyClientConfigForScreenshot();
            // This is a workaround for the issue that the screenshot will be taken before CommentWorldRenderer is hidden
            minecraft.tell(() -> RenderSystem.recordRenderCall(() -> minecraft.tell(() -> RenderSystem.recordRenderCall(() -> {
                grabScreenshot(imageBytes -> minecraft.execute(() -> {
                    minecraft.player.playSound(shutterSoundEvent);
                    Minecraft.getInstance().setScreen(new CommentToolScreen(imageBytes));
                }));
                minecraft.options.hideGui = prevHideGui;
                MainClient.CLIENT_CONFIG.commentVisibilityMask = true;
                isGrabbing = false;
            }))));
        }
        return true;
    }

    public static void applyClientConfigForScreenshot() {
        isGrabbing = true;
        Minecraft.getInstance().options.hideGui = !MainClient.CLIENT_CONFIG.screenshotIncludeGui;
        MainClient.CLIENT_CONFIG.commentVisibilityMask = MainClient.CLIENT_CONFIG.screenshotIncludeComments;
    }
}
