package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

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
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x1 = getX(), x2 = getX() + getWidth();
        int y1 = getY(), y2 = getY() + getHeight();

        int shadowColor = 0xFF333333;
        int shadowOffset = 2;
        guiGraphics.fill(
                (int) (x1 + shadowOffset), (int) (y1 + shadowOffset),
                (int) (x2 + shadowOffset), (int) (y2 + shadowOffset),
                shadowColor
        );

        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(0, 0).endVertex();
        bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public void close() {
        texture.close();
    }
}
