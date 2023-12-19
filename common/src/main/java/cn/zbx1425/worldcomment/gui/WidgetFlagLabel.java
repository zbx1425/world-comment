package cn.zbx1425.worldcomment.gui;

#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

public class WidgetFlagLabel extends WidgetLabel implements IGuiCommon {

    public int color;

    public WidgetFlagLabel(int x, int y, int width, int height, int color, Component text) {
        super(x, y, width, height, text);
        this.color = color;
        this.padding = 6;
    }

    @Override
#if MC_VERSION >= "12000"
    protected void renderWidget(GuiGraphics guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = guiParam;
#else
    public void render(PoseStack guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = GuiGraphics.withPose(guiParam);
#endif
        guiGraphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f, 1);
        guiGraphics.blit(ATLAS_LOCATION, getX(), getY(), 10, getHeight(),
                0, 48, 20, 10, 256, 256);
        guiGraphics.blit(ATLAS_LOCATION, getX() + 10, getY(), getWidth() - 20, getHeight(),
                10, 48, 108, 10, 256, 256);
        guiGraphics.blit(ATLAS_LOCATION, getX() + getWidth() - 10, getY(), 10, getHeight(),
                118, 48, 10, 10, 256, 256);
        guiGraphics.setColor(1, 1, 1, 1);
#if MC_VERSION >= "12000"
        super.renderWidget(guiParam, mouseX, mouseY, partialTick);
#else
        super.render(guiParam, mouseX, mouseY, partialTick);
#endif
    }
}
