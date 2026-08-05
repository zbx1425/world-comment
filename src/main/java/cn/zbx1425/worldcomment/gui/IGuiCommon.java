package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public interface IGuiCommon {

    int SQ_SIZE = 20;
    Identifier ATLAS_LOCATION = Main.id("textures/gui/comment-tool.png");
    int ATLAS_SIZE = 256;


}
