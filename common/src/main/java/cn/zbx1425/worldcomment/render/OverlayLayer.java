package cn.zbx1425.worldcomment.render;

import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif

public class OverlayLayer {

    public static void render(GuiGraphics guiGraphics) {
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
