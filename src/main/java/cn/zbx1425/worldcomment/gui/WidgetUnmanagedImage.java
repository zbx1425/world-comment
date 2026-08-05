package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
//? if >=1.19.3
import org.joml.Matrix4f;
//? if <1.19.3
//import com.mojang.math.Matrix4f;

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
//? if >=1.20 {
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
//? } else {
    /*public void render(GuiGraphics guiParam, int mouseX, int mouseY, float partialTick) {
        super.render(guiParam, mouseX, mouseY, partialTick);
*///? }
        ISnGuiCanvas guiGraphics = ISnGuiCanvas.fromGuiParam(guiParam);
        int x1 = getX(), x2 = getX() + getWidth();
        int y1 = getY(), y2 = getY() + getHeight();

        int shadowColor = 0xFF404040;
        int shadowOffset = 2;
        guiGraphics.fill(
                (int) (x1), (int) (y1 + shadowOffset),
                (int) (x2), (int) (y2 + shadowOffset),
                shadowColor
        );
        guiGraphics.blit(texture, x1, y1, x2, y2);
    }

    @Override
//? if >=1.20 {
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
//? } else {
    /*public void updateNarration(NarrationElementOutput narrationElementOutput) { }
*///? }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public void close() {
        texture.close();
    }

//? if <1.20 {
    /*private int getX() { return x; }
    private int getY() { return y; }
    private void setX(int x) { this.x = x; }
    private void setY(int y) { this.y = y; }
*///? }
}
