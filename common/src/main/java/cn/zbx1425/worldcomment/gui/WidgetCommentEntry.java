package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.data.CommentEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class WidgetCommentEntry extends AbstractWidget {

    private final CommentEntry comment;

    public static final int TOP_SINK = 10;

    public WidgetCommentEntry(int x, int y, int width, CommentEntry comment) {
        super(x, y, width, 0, Component.literal(comment.message));
        this.comment = comment;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
