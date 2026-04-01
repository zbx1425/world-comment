package cn.zbx1425.worldcomment.gui.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

#if MC_VERSION >= "12000" && MC_VERSION < "12106"

public class SnGuiGraphics120 implements ISnGuiGraphics {

    private GuiGraphicsExtractor sink;
    private int currentColor = -1;

    private static SnGuiGraphics120 INSTANCE = null;

    private SnGuiGraphics120(GuiGraphicsExtractor sink) {
        this.sink = sink;
    }

    public static SnGuiGraphics120 withGuiParam(GuiGraphicsExtractor sink) {
        if (INSTANCE == null) {
            INSTANCE = new SnGuiGraphics120(sink);
        } else {
            INSTANCE.sink = sink;
        }
        return INSTANCE;
    }

    @Override
    public GuiGraphicsExtractor getGuiParam() {
        return sink;
    }

    @Override
    public void blit(Identifier atlasLocation, int x, int y, int padLeft, int padTop, int uOffset, int vOffset, int padLeft1, int padTop1, int texWidth, int texHeight) {
        sink.blit(atlasLocation, x, y, uOffset, vOffset, padLeft, padTop, padLeft1, padTop1, texWidth, texHeight, currentColor);
    }

    @Override
    public void blit(Identifier atlasLocation, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        sink.blit(atlasLocation, x, y, uOffset, vOffset, uWidth, vHeight);
    }

    @Override
    public void fill(int i, int i1, int i2, int i3, int shadowColor) {
        sink.fill(i, i1, i2, i3, shadowColor);
    }

    @Override
    public void enableScissor(int i, int i1, int i2, int i3) {
        sink.enableScissor(i, i1, i2, i3);
    }

    @Override
    public void disableScissor() {
        sink.disableScissor();
    }

    @Override
    public void drawString(Font var1, String var2, int var3, int var4, int var5, boolean var6) {
        sink.drawString(var1, var2, var3, var4, var5, var6);
    }

    @Override
    public void drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5, boolean var6) {
        sink.drawString(var1, var2, var3, var4, var5, var6);
    }

    @Override
    public void drawString(Font var1, Component var2, int var3, int var4, int var5) {
        sink.drawString(var1, var2, var3, var4, var5);
    }

    @Override
    public void drawString(Font var1, Component var2, int var3, int var4, int var5, boolean var6) {
        sink.drawString(var1, var2, var3, var4, var5, var6);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        sink.setColor(r, g, b, a);
    }

    @Override
    public void drawCenteredString(Font font, Component translatedKeyMessage, int i, int i1, int i2) {
        sink.drawCenteredString(font, translatedKeyMessage, i, i1, i2);
    }

    @Override
    public void renderTooltip(Font font, MutableComponent translatable, int mouseX, int mouseY) {
        sink.renderTooltip(font, translatable, mouseX, mouseY);
    }

    @Override
    public void renderTooltip(Font font, List<Component> append, Optional<TooltipComponent> empty, int mouseX, int mouseY) {
        sink.renderTooltip(font, append, empty, mouseX, mouseY);
    }

    @Override
    public int guiWidth() {
        return sink.guiWidth();
    }

    @Override
    public int guiHeight() {
        return sink.guiHeight();
    }

    @Override
    public void blit(AbstractTexture texture, int x1, int y1, int x2, int y2) {
        RenderSystem.setShaderTexture(0, texture.getId());

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = sink.pose().last().pose();
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
    public void pushPose() {
        sink.pose().pushMatrix();
    }

    @Override
    public void popPose() {
        sink.pose().popMatrix();
    }

    @Override
    public void translate(float x, float y, float z) {
        sink.pose().translate(x, y);
    }

    @Override
    public void scale(float x, float y) {
        sink.pose().scale(x, y);
    }

    @Override
    public void enableBlend() {
        RenderSystem.enableBlend();
    }

    @Override
    public void disableBlend() {
        RenderSystem.disableBlend();
    }
}

#endif
