package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
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
    protected void renderWidget(GuiGraphics guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = guiParam;
#else
    public void render(PoseStack guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = GuiGraphics.withPose(guiParam);
        super.render(guiParam, mouseX, mouseY, partialTick);
#endif
        int x1 = getX(), x2 = getX() + getWidth();
        int y1 = getY(), y2 = getY() + getHeight();

        int shadowColor = 0xFF111111;
        int shadowOffset = 2;
        guiGraphics.fill(
                (int) (x1 + shadowOffset), (int) (y1 + shadowOffset),
                (int) (x2 + shadowOffset), (int) (y2 + shadowOffset),
                shadowColor
        );

        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
#if MC_VERSION >= "12100"
        BufferBuilder bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, x1, y1, 0).setUv(0, 0);
        bufferBuilder.addVertex(matrix4f, x1, y2, 0).setUv(0, 1);
        bufferBuilder.addVertex(matrix4f, x2, y2, 0).setUv(1, 1);
        bufferBuilder.addVertex(matrix4f, x2, y1, 0).setUv(1, 0);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
#else
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(0, 0).endVertex();
        bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
#endif
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
