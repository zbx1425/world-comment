package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public interface IGuiCommon {

    ResourceLocation ATLAS_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/comment-tool.png");

    default void graphicsBlit9(GuiGraphics guiGraphics, int x, int y, int width, int height,
            int uOffset, int vOffset, int uWidth, int vHeight, int texWidth, int texHeight,
            int padTop, int padRight, int padBottom, int padLeft) {
        if (padTop > 0) {
            guiGraphics.blit(ATLAS_LOCATION,
                    x, y, padLeft, padTop,
                    uOffset, vOffset, padLeft, padTop,
                    texWidth, texHeight
            );
            guiGraphics.blit(ATLAS_LOCATION,
                    x + padLeft, y, width - padLeft - padRight, padTop,
                    uOffset + padLeft, vOffset, uWidth - padLeft - padRight, padTop,
                    texWidth, texHeight
            );
            guiGraphics.blit(ATLAS_LOCATION,
                    x + width - padRight, y, padRight, padTop,
                    uOffset + uWidth - padRight, vOffset, padRight, padTop,
                    texWidth, texHeight
            );
        }

        guiGraphics.blit(ATLAS_LOCATION,
                x, y + padTop, padLeft, height - padTop - padBottom,
                uOffset, vOffset + padTop, padLeft, vHeight - padTop - padBottom,
                texWidth, texHeight
        );
        guiGraphics.blit(ATLAS_LOCATION,
                x + padLeft, y + padTop, width - padLeft - padRight, height - padTop - padBottom,
                uOffset + padLeft, vOffset + padTop, uWidth - padLeft - padRight, vHeight - padTop - padBottom,
                texWidth, texHeight
        );
        guiGraphics.blit(ATLAS_LOCATION,
                x + width - padRight, y + padTop, padRight, height - padTop - padBottom,
                uOffset + uWidth - padRight, vOffset + padTop, padRight, vHeight - padTop - padBottom,
                texWidth, texHeight
        );

        if (padBottom > 0) {
            guiGraphics.blit(ATLAS_LOCATION,
                    x, y + height - padBottom, padLeft, padBottom,
                    uOffset, vOffset + vHeight - padBottom, padLeft, padBottom,
                    texWidth, texHeight
            );
            guiGraphics.blit(ATLAS_LOCATION,
                    x + padLeft, y + height - padBottom, width - padLeft - padRight, padBottom,
                    uOffset + padLeft, vOffset + vHeight - padBottom, uWidth - padLeft - padRight, padBottom,
                    texWidth, texHeight
            );
            guiGraphics.blit(ATLAS_LOCATION,
                    x + width - padRight, y + height - padBottom, padRight, padBottom,
                    uOffset + uWidth - padRight, vOffset + vHeight - padBottom, padRight, padBottom,
                    texWidth, texHeight
            );
        }
    }

#if MC_VERSION < "12000"

    default void renderScrollingString(GuiGraphics var0, Font var1, Component var2, int var3, int var4, int var5, int var6, int var7) {
        int var8 = var1.width(var2);
        int var9 = (var4 + var6 - 9) / 2 + 1;
        int var10 = var5 - var3;
        if (var8 > var10) {
            int var11 = var8 - var10;
            double var12 = (double) Util.getMillis() / 1000.0;
            double var14 = Math.max((double)var11 * 0.5, 3.0);
            double var16 = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * var12 / var14)) / 2.0 + 0.5;
            double var18 = Mth.lerp(var16, 0.0, (double)var11);
            var0.enableScissor(var3, var4, var5, var6);
            var0.drawString(var1, var2, var3 - (int)var18, var9, var7);
            var0.disableScissor();
        } else {
            var0.drawCenteredString(var1, var2, (var3 + var5) / 2, var9, var7);
        }
    }
#endif
}
