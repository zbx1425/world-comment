package cn.zbx1425.worldcomment.gui.compat;

import cn.zbx1425.worldcomment.mixin.GuiGraphicsAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public class SnGuiGraphics12106 implements ISnGuiGraphics {

    private GuiGraphics sink;

    private static SnGuiGraphics12106 INSTANCE = null;

    private SnGuiGraphics12106(GuiGraphics sink) {
        this.sink = sink;
    }

    public static SnGuiGraphics12106 withGuiParam(GuiGraphics sink) {
        if (INSTANCE == null) {
            INSTANCE = new SnGuiGraphics12106(sink);
        } else {
            INSTANCE.sink = sink;
        }
        return INSTANCE;
    }

    @Override
    public GuiGraphics getGuiParam() {
        return sink;
    }

    @Override
    public void blit(ResourceLocation atlasLocation, int x, int y, int padLeft, int padTop, int uOffset, int vOffset, int padLeft1, int padTop1, int texWidth, int texHeight) {
        sink.blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, padLeft, padTop, uOffset, vOffset, padLeft1, padTop1, texWidth, texHeight);
    }

    @Override
    public void blit(ResourceLocation var1, int var10, int var14, int i, int i1, int var12, int var16) {
        sink.blit(RenderPipelines.GUI_TEXTURED, var1, var10, var14, i, i1, var12, var16);
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
    public void setColor(float v, float v1, float v2, float v3) {

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
        ((GuiGraphicsAccessor)sink).invokeSubmitBlit(RenderPipelines.GUI_TEXTURED, texture.getTextureView(),
                x1, y1, x2, y2, 0, 1, 0, 1, -1);
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
        
    }

    @Override
    public void disableBlend() {

    }
}
