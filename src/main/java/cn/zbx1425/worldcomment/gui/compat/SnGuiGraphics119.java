package cn.zbx1425.worldcomment.gui.compat;

#if MC_VERSION < "12000"

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class SnGuiGraphics119 implements ISnGuiGraphics {

    private final Minecraft minecraft;
    private final MultiBufferSource.BufferSource bufferSource;
    private PoseStack pose;

    private static SnGuiGraphics119 INSTANCE = null;

    private SnGuiGraphics119(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        this.minecraft = minecraft;
        this.pose = poseStack;
        this.bufferSource = bufferSource;
    }

    public static SnGuiGraphics119 withGuiParam(PoseStack poseStack) {
        if (INSTANCE == null) {
            INSTANCE = new SnGuiGraphics119(Minecraft.getInstance(), poseStack, MultiBufferSource.immediate(Tesselator.getInstance().getBuilder()));
        } else {
            INSTANCE.pose = poseStack;
        }
        return INSTANCE;
    }

    public PoseStack pose() {
        return pose;
    }

    public void blit(ResourceLocation atlasLocation, int x, int y, int padLeft, int padTop, int uOffset, int vOffset, int padLeft1, int padTop1, int texWidth, int texHeight) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        GuiComponent.blit(pose, x, y, padLeft, padTop, uOffset, vOffset, padLeft1, padTop1, texWidth, texHeight);
    }

    private void blit(ResourceLocation var1, int var10, int var14, int i, int i1, int var12, int var16) {
        RenderSystem.setShaderTexture(0, var1);
        GuiComponent.blit(pose, var10, var14, i, i1, var12, var16, 256, 256);
    }

    public void fill(int i, int i1, int i2, int i3, int shadowColor) {
        GuiComponent.fill(pose, i, i1, i2, i3, shadowColor);
    }

    public void enableScissor(int i, int i1, int i2, int i3) {
        GuiComponent.enableScissor(i, i1, i2, i3);
    }

    public void disableScissor() {
        GuiComponent.disableScissor();
    }

    public void drawString(Font var1, String var2, int var3, int var4, int var5, boolean var6) {
        if (var2 == null) return 0;
        int var7 = var1.drawInBatch(var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, false, 0, 15728880, var1.isBidirectional());
        this.flush();
    }

    public void drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5, boolean var6) {
        int var7 = var1.drawInBatch(var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, false, 0, 15728880);
        this.flush();
    }

    public void drawString(Font var1, Component var2, int var3, int var4, int var5) {
        this.drawString(var1, var2, var3, var4, var5, true);
    }

    public void drawString(Font var1, Component var2, int var3, int var4, int var5, boolean var6) {
        this.drawString(var1, var2.getVisualOrderText(), var3, var4, var5, var6);
    }

    public void setColor(float v, float v1, float v2, float v3) {
        RenderSystem.setShaderColor(v, v1, v2, v3);
    }

    public void drawCenteredString(Font font, Component translatedKeyMessage, int i, int i1, int i2) {
        GuiComponent.drawCenteredString(pose, font, translatedKeyMessage, i, i1, i2);
    }

    private static class DummyScreen extends Screen {

        public static final DummyScreen INSTANCE = new DummyScreen();

        protected DummyScreen() {
            super(Component.literal(""));
        }
    }

    public void renderTooltip(Font font, MutableComponent translatable, int mouseX, int mouseY) {
        DummyScreen.INSTANCE.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        DummyScreen.INSTANCE.renderTooltip(pose, translatable, mouseX, mouseY);
    }

    public void renderTooltip(Font font, List<Component> append, Optional<TooltipComponent> empty, int mouseX, int mouseY) {
        DummyScreen.INSTANCE.init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        DummyScreen.INSTANCE.renderTooltip(pose, append, empty, mouseX, mouseY);
    }

    public int guiWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    public void flush() {
        RenderSystem.disableDepthTest();
        this.bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void blit(int glId, int x1, int y1, int x2, int y2) {
        RenderSystem.setShaderTexture(0, imageToDraw.getFriendlyTexture(minecraft.getTextureManager()).getId());

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

}

#endif
