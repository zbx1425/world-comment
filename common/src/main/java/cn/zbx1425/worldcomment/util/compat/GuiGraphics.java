package cn.zbx1425.worldcomment.util.compat;

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

public class GuiGraphics {

    private final Minecraft minecraft;
    private final MultiBufferSource.BufferSource bufferSource;
    private PoseStack pose;

    private static GuiGraphics INSTANCE = null;

    public GuiGraphics(Minecraft minecraft, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        this.minecraft = minecraft;
        this.pose = poseStack;
        this.bufferSource = bufferSource;
    }

    public static GuiGraphics withPose(PoseStack poseStack) {
        if (INSTANCE == null) {
            INSTANCE = new GuiGraphics(Minecraft.getInstance(), poseStack, MultiBufferSource.immediate(Tesselator.getInstance().getBuilder()));
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

    public void fill(int i, int i1, int i2, int i3, int shadowColor) {
        GuiComponent.fill(pose, i, i1, i2, i3, shadowColor);
    }

    public void enableScissor(int i, int i1, int i2, int i3) {
        GuiComponent.enableScissor(i, i1, i2, i3);
    }

    public void disableScissor() {
        GuiComponent.disableScissor();
    }

    public int drawString(Font var1, String var2, int var3, int var4, int var5, boolean var6) {
        if (var2 == null) return 0;
        int var7 = var1.drawInBatch(var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, false, 0, 15728880, var1.isBidirectional());
        this.flush();
        return var7;
    }

    public int drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5, boolean var6) {
        int var7 = var1.drawInBatch(var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, false, 0, 15728880);
        this.flush();
        return var7;
    }

    public int drawString(Font var1, Component var2, int var3, int var4, int var5) {
        return this.drawString(var1, var2, var3, var4, var5, true);
    }

    public int drawString(Font var1, Component var2, int var3, int var4, int var5, boolean var6) {
        return this.drawString(var1, var2.getVisualOrderText(), var3, var4, var5, var6);
    }

    public void setColor(float v, float v1, float v2, float v3) {
        RenderSystem.setShaderColor(v, v1, v2, v3);
    }

    public void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
        this.blitNineSliced(var1, var2, var3, var4, var5, var6, var6, var6, var6, var7, var8, var9, var10);
    }

    public void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
        this.blitNineSliced(var1, var2, var3, var4, var5, var6, var7, var6, var7, var8, var9, var10, var11);
    }

    public void blitNineSliced(
            ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13
    ) {
        var6 = Math.min(var6, var4 / 2);
        var8 = Math.min(var8, var4 / 2);
        var7 = Math.min(var7, var5 / 2);
        var9 = Math.min(var9, var5 / 2);
        if (var4 == var10 && var5 == var11) {
            this.blit(var1, var2, var3, var12, var13, var4, var5);
        } else if (var5 == var11) {
            this.blit(var1, var2, var3, var12, var13, var6, var5);
            this.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var5, var12 + var6, var13, var10 - var8 - var6, var11);
            this.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var5);
        } else if (var4 == var10) {
            this.blit(var1, var2, var3, var12, var13, var4, var7);
            this.blitRepeating(var1, var2, var3 + var7, var4, var5 - var9 - var7, var12, var13 + var7, var10, var11 - var9 - var7);
            this.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var4, var9);
        } else {
            this.blit(var1, var2, var3, var12, var13, var6, var7);
            this.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var7, var12 + var6, var13, var10 - var8 - var6, var7);
            this.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var7);
            this.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var6, var9);
            this.blitRepeating(var1, var2 + var6, var3 + var5 - var9, var4 - var8 - var6, var9, var12 + var6, var13 + var11 - var9, var10 - var8 - var6, var9);
            this.blit(var1, var2 + var4 - var8, var3 + var5 - var9, var12 + var10 - var8, var13 + var11 - var9, var8, var9);
            this.blitRepeating(var1, var2, var3 + var7, var6, var5 - var9 - var7, var12, var13 + var7, var6, var11 - var9 - var7);
            this.blitRepeating(
                    var1, var2 + var6, var3 + var7, var4 - var8 - var6, var5 - var9 - var7, var12 + var6, var13 + var7, var10 - var8 - var6, var11 - var9 - var7
            );
            this.blitRepeating(var1, var2 + var4 - var8, var3 + var7, var6, var5 - var9 - var7, var12 + var10 - var8, var13 + var7, var8, var11 - var9 - var7);
        }
    }

    public void blitRepeating(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
        int var10 = var2;

        int var12;
        for(IntIterator var11 = slices(var4, var8); var11.hasNext(); var10 += var12) {
            var12 = var11.nextInt();
            int var13 = (var8 - var12) / 2;
            int var14 = var3;

            int var16;
            for(IntIterator var15 = slices(var5, var9); var15.hasNext(); var14 += var16) {
                var16 = var15.nextInt();
                int var17 = (var9 - var16) / 2;
                this.blit(var1, var10, var14, var6 + var13, var7 + var17, var12, var16);
            }
        }
    }

    private void blit(ResourceLocation var1, int var10, int var14, int i, int i1, int var12, int var16) {
        RenderSystem.setShaderTexture(0, var1);
        GuiComponent.blit(pose, var10, var14, i, i1, var12, var16, 256, 256);
    }


    private static IntIterator slices(int var0, int var1) {
        int var2 = Mth.positiveCeilDiv(var0, var1);
        return new Divisor(var0, var2);
    }

    public void drawCenteredString(Font font, Component translatedKeyMessage, int i, int i1, int i2) {
        GuiComponent.drawCenteredString(pose, font, translatedKeyMessage, i, i1, i2);
    }

    private static class Divisor implements IntIterator {
        private final int denominator;
        private final int quotient;
        private final int mod;
        private int returnedParts;
        private int remainder;

        public Divisor(int var1, int var2) {
            this.denominator = var2;
            if (var2 > 0) {
                this.quotient = var1 / var2;
                this.mod = var1 % var2;
            } else {
                this.quotient = 0;
                this.mod = 0;
            }

        }

        public boolean hasNext() {
            return this.returnedParts < this.denominator;
        }

        @Override
        public int nextInt() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                int var1 = this.quotient;
                this.remainder += this.mod;
                if (this.remainder >= this.denominator) {
                    this.remainder -= this.denominator;
                    ++var1;
                }

                ++this.returnedParts;
                return var1;
            }
        }
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

}
