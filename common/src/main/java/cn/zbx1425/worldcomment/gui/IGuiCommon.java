package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public interface IGuiCommon {

    int SQ_SIZE = 20;
    ResourceLocation ATLAS_LOCATION = Main.id("textures/gui/comment-tool.png");

}
