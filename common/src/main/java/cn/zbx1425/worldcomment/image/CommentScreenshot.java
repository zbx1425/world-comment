package cn.zbx1425.worldcomment.image;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class CommentScreenshot {

    public static void grab(Consumer<Path> callback) {
        Minecraft.getInstance().execute(() -> {
            File targetFile = getAvailableFile();
            Screenshot.grab(Minecraft.getInstance().gameDirectory, targetFile.getName(),
                    Minecraft.getInstance().getMainRenderTarget(),
                    ignored -> {
                        callback.accept(targetFile.toPath());
            });
        });
    }

    private static File getAvailableFile() {
        File screenShotDirectory = new File(Minecraft.getInstance().gameDirectory, Screenshot.SCREENSHOT_DIR);
        String s = "WorldComment-" + Util.getFilenameFormattedDateTime();
        int i = 1;
        File file1;
        while ((file1 = new File(screenShotDirectory, s + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
            ++i;
        }
        return file1;
    }
}
