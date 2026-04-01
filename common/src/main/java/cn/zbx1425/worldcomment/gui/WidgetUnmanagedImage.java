package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; #endif
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
#if MC_VERSION >= "11903" import org.joml.Matrix4f; #else import com.mojang.math.Matrix4f; #endif

public class WidgetUnmanagedImage extends AbstractWidget implements AutoCloseable {

    private final DynamicTexture texture;

    public WidgetUnmanagedImage(DynamicTexture texture) {
        super(0, 0, 0, 0, Component.empty());
        this.texture = texture;
    }

    public void setBounds(int i, int j, int width) {
        setX(i);
        setY(j);
        setWidth(width);
        height = (int)(width * 1f / texture.getPixels().getWidth() * texture.getPixels().getHeight());
    }

    @Override
#if MC_VERSION >= "12000"
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
#else
    public void render(PoseStack guiParam, int mouseX, int mouseY, float partialTick) {
        super.render(guiParam, mouseX, mouseY, partialTick);
#endif
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        int x1 = getX(), x2 = getX() + getWidth();
        int y1 = getY(), y2 = getY() + getHeight();

        int shadowColor = 0xFF111111;
        int shadowOffset = 2;
        guiGraphics.fill(
                (int) (x1 + shadowOffset), (int) (y1 + shadowOffset),
                (int) (x2 + shadowOffset), (int) (y2 + shadowOffset),
                shadowColor
        );
        guiGraphics.blit(texture, x1, y1, x2, y2);
    }

    @Override
#if MC_VERSION >= "12000"
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
#else
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }
#endif

    @Override
    public void close() {
        texture.close();
    }

#if MC_VERSION < "12000"
    private int getX() { return x; }
    private int getY() { return y; }
    private void setX(int x) { this.x = x; }
    private void setY(int y) { this.y = y; }
#endif
}
