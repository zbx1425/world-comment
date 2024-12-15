package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class Screenshot {

    public static void grabScreenshot(Consumer<byte[]> callback) {
        RenderTarget frameBuf = Minecraft.getInstance().getMainRenderTarget();
        NativeImage fullSizeImage = new NativeImage(frameBuf.width, frameBuf.height, false);
        try (fullSizeImage) {
            RenderSystem.bindTexture(frameBuf.getColorTextureId());
            fullSizeImage.downloadTexture(0, true);
            fullSizeImage.flipY();
            if (fullSizeImage.getWidth() > 2000) {
                int newWidth = 1920;
                int newHeight = (int) (fullSizeImage.getHeight() * (newWidth / (double) fullSizeImage.getWidth()));
                NativeImage scaledImage = new NativeImage(newWidth, newHeight, false);
                try (scaledImage) {
                    fullSizeImage.resizeSubRectTo(0, 0, fullSizeImage.getWidth(), fullSizeImage.getHeight(), scaledImage);
                    callback.accept(scaledImage.asByteArray());
                    return;
                }
            }
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
            minecraft.tell(() -> {
                boolean prevHideGui = minecraft.options.hideGui;
                minecraft.options.hideGui = !minecraft.options.keySprint.isDown();
                RenderSystem.recordRenderCall(() -> {
                    grabScreenshot(imageBytes -> minecraft.execute(() -> {
                        minecraft.player.playSound(shutterSoundEvent);
                        Minecraft.getInstance().setScreen(new CommentToolScreen(imageBytes));
                    }));
                    minecraft.options.hideGui = prevHideGui;
                });
            });
        }
        return true;
    }
}
