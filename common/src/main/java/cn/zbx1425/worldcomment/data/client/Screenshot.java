package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Screenshot {

    public static void grabScreenshot(Consumer<Path> callback) {
        OverlayLayer.isTakingScreenshot = true;
        Minecraft.getInstance().execute(() -> {
            File targetFile = getAvailableFile();
            net.minecraft.client.Screenshot.grab(Minecraft.getInstance().gameDirectory, targetFile.getName(),
                    Minecraft.getInstance().getMainRenderTarget(),
                    ignored -> {
                        callback.accept(targetFile.toPath());
                    });
            OverlayLayer.isTakingScreenshot = false;
        });
    }

    private static File getAvailableFile() {
        File screenShotDirectory = new File(Minecraft.getInstance().gameDirectory, net.minecraft.client.Screenshot.SCREENSHOT_DIR);
        String s = "WorldComment-" + Util.getFilenameFormattedDateTime();
        int i = 1;
        File file1;
        while ((file1 = new File(screenShotDirectory, s + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
            ++i;
        }
        return file1;
    }

    private static final SoundEvent shutterSoundEvent = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("worldcomment:shutter"), 16
    );

    public static boolean handleKeyF2() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        ItemStack item = minecraft.player.getMainHandItem();
        if (!item.is(Main.ITEM_COMMENT_TOOL.get())) return false;
        if (item.getOrCreateTag().contains("uploadJobId", Tag.TAG_LONG)) return false;

        if (minecraft.screen == null) {
            grabScreenshot(path -> {
                Minecraft.getInstance().execute(() -> {
                    minecraft.player.playSound(shutterSoundEvent);
                    Minecraft.getInstance().setScreen(new CommentToolScreen(path));
                });
            });
        }
        return true;
    }
}
