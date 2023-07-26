package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

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
}
