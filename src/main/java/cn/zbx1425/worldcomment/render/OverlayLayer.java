package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import net.minecraft.client.Minecraft;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;

public class OverlayLayer {

    public static void render(ISnGuiCanvas guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (minecraft.options.hideGui) return;
        if (!minecraft.options.getCameraType().isFirstPerson()) return;

        CommentOverlayRenderer.render(guiGraphics);
        // if (!isTakingScreenshot) {
        ControlTipRenderer.render(guiGraphics);
        // }
    }
}
