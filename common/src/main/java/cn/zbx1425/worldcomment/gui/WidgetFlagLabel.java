package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WidgetFlagLabel extends WidgetLabel {

    private static final ResourceLocation ATLAS_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/comment-tool.png");

    public int color;

    public WidgetFlagLabel(int x, int y, int width, int height, int color, Component text) {
        super(x, y, width, height, text);
        this.color = color;
        this.padding = 6;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f, 1);
        guiGraphics.blit(ATLAS_LOCATION, getX(), getY(), 10, getHeight(),
                0, 48, 20, 10, 256, 256);
        guiGraphics.blit(ATLAS_LOCATION, getX() + 10, getY(), getWidth() - 20, getHeight(),
                10, 48, 108, 10, 256, 256);
        guiGraphics.blit(ATLAS_LOCATION, getX() + getWidth() - 10, getY(), 10, getHeight(),
                118, 48, 10, 10, 256, 256);
        guiGraphics.setColor(1, 1, 1, 1);
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);
    }
}
