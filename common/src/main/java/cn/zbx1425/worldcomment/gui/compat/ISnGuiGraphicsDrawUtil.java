package cn.zbx1425.worldcomment.gui.compat;

import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.NoSuchElementException;

public interface ISnGuiGraphicsDrawUtil {

    default void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
        this.blitNineSliced(var1, var2, var3, var4, var5, var6, var6, var6, var6, var7, var8, var9, var10);
    }

    default void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
        this.blitNineSliced(var1, var2, var3, var4, var5, var6, var7, var6, var7, var8, var9, var10, var11);
    }

    default void blitNineSliced(
            ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13
    ) {
        ISnGuiGraphics guiGraphics = (ISnGuiGraphics) this;
        var6 = Math.min(var6, var4 / 2);
        var8 = Math.min(var8, var4 / 2);
        var7 = Math.min(var7, var5 / 2);
        var9 = Math.min(var9, var5 / 2);
        if (var4 == var10 && var5 == var11) {
            guiGraphics.blit(var1, var2, var3, var12, var13, var4, var5);
        } else if (var5 == var11) {
            guiGraphics.blit(var1, var2, var3, var12, var13, var6, var5);
            guiGraphics.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var5, var12 + var6, var13, var10 - var8 - var6, var11);
            guiGraphics.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var5);
        } else if (var4 == var10) {
            guiGraphics.blit(var1, var2, var3, var12, var13, var4, var7);
            guiGraphics.blitRepeating(var1, var2, var3 + var7, var4, var5 - var9 - var7, var12, var13 + var7, var10, var11 - var9 - var7);
            guiGraphics.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var4, var9);
        } else {
            guiGraphics.blit(var1, var2, var3, var12, var13, var6, var7);
            guiGraphics.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var7, var12 + var6, var13, var10 - var8 - var6, var7);
            guiGraphics.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var7);
            guiGraphics.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var6, var9);
            guiGraphics.blitRepeating(var1, var2 + var6, var3 + var5 - var9, var4 - var8 - var6, var9, var12 + var6, var13 + var11 - var9, var10 - var8 - var6, var9);
            guiGraphics.blit(var1, var2 + var4 - var8, var3 + var5 - var9, var12 + var10 - var8, var13 + var11 - var9, var8, var9);
            guiGraphics.blitRepeating(var1, var2, var3 + var7, var6, var5 - var9 - var7, var12, var13 + var7, var6, var11 - var9 - var7);
            guiGraphics.blitRepeating(
                    var1, var2 + var6, var3 + var7, var4 - var8 - var6, var5 - var9 - var7, var12 + var6, var13 + var7, var10 - var8 - var6, var11 - var9 - var7
            );
            guiGraphics.blitRepeating(var1, var2 + var4 - var8, var3 + var7, var6, var5 - var9 - var7, var12 + var10 - var8, var13 + var7, var8, var11 - var9 - var7);
        }
    }

    default void blitRepeating(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
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
                ((ISnGuiGraphics) this).blit(var1, var10, var14, var6 + var13, var7 + var17, var12, var16);
            }
        }
    }


    default void blitNineSlicedFast(ResourceLocation atlasLocation, int x, int y, int width, int height,
                               int uOffset, int vOffset, int uWidth, int vHeight, int texWidth, int texHeight,
                               int padTop, int padRight, int padBottom, int padLeft) {
        ISnGuiGraphics guiGraphics = (ISnGuiGraphics) this;
        if (padTop > 0) {
            guiGraphics.blit(atlasLocation,
                    x, y, padLeft, padTop,
                    uOffset, vOffset, padLeft, padTop,
                    texWidth, texHeight
            );
            guiGraphics.blit(atlasLocation,
                    x + padLeft, y, width - padLeft - padRight, padTop,
                    uOffset + padLeft, vOffset, uWidth - padLeft - padRight, padTop,
                    texWidth, texHeight
            );
            guiGraphics.blit(atlasLocation,
                    x + width - padRight, y, padRight, padTop,
                    uOffset + uWidth - padRight, vOffset, padRight, padTop,
                    texWidth, texHeight
            );
        }

        guiGraphics.blit(atlasLocation,
                x, y + padTop, padLeft, height - padTop - padBottom,
                uOffset, vOffset + padTop, padLeft, vHeight - padTop - padBottom,
                texWidth, texHeight
        );
        guiGraphics.blit(atlasLocation,
                x + padLeft, y + padTop, width - padLeft - padRight, height - padTop - padBottom,
                uOffset + padLeft, vOffset + padTop, uWidth - padLeft - padRight, vHeight - padTop - padBottom,
                texWidth, texHeight
        );
        guiGraphics.blit(atlasLocation,
                x + width - padRight, y + padTop, padRight, height - padTop - padBottom,
                uOffset + uWidth - padRight, vOffset + padTop, padRight, vHeight - padTop - padBottom,
                texWidth, texHeight
        );

        if (padBottom > 0) {
            guiGraphics.blit(atlasLocation,
                    x, y + height - padBottom, padLeft, padBottom,
                    uOffset, vOffset + vHeight - padBottom, padLeft, padBottom,
                    texWidth, texHeight
            );
            guiGraphics.blit(atlasLocation,
                    x + padLeft, y + height - padBottom, width - padLeft - padRight, padBottom,
                    uOffset + padLeft, vOffset + vHeight - padBottom, uWidth - padLeft - padRight, padBottom,
                    texWidth, texHeight
            );
            guiGraphics.blit(atlasLocation,
                    x + width - padRight, y + height - padBottom, padRight, padBottom,
                    uOffset + uWidth - padRight, vOffset + vHeight - padBottom, padRight, padBottom,
                    texWidth, texHeight
            );
        }
    }

    default void renderScrollingString(Font var1, Component var2, int var3, int var4, int var5, int var6, int var7) {
        ISnGuiGraphics guiGraphics = (ISnGuiGraphics) this;
        int var8 = var1.width(var2);
        int var9 = (var4 + var6 - 9) / 2 + 1;
        int var10 = var5 - var3;
        if (var8 > var10) {
            int var11 = var8 - var10;
            double var12 = (double) Util.getMillis() / 1000.0;
            double var14 = Math.max((double)var11 * 0.5, 3.0);
            double var16 = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * var12 / var14)) / 2.0 + 0.5;
            double var18 = Mth.lerp(var16, 0.0, (double)var11);
            guiGraphics.enableScissor(var3, var4, var5, var6);
            guiGraphics.drawString(var1, var2, var3 - (int)var18, var9, var7);
            guiGraphics.disableScissor();
        } else {
            guiGraphics.drawCenteredString(var1, var2, (var3 + var5) / 2, var9, var7);
        }
    }

    private static IntIterator slices(int var0, int var1) {
        int var2 = Mth.positiveCeilDiv(var0, var1);
        return new Divisor(var0, var2);
    }

    class Divisor implements IntIterator {
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

}
