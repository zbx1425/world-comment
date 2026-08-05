package cn.zbx1425.worldcomment.gui.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;
import java.util.Optional;

public interface ISnGuiCanvas extends ISnGuiCanvasDrawUtil {

//? if >=1.20 {
    GuiGraphicsExtractor getGuiParam();
//? } else {
    /*PoseStack getGuiParam();
*///? }

    void blit(Identifier atlasLocation, int x, int y, int padLeft, int padTop, int uOffset, int vOffset, int padLeft1, int padTop1, int texWidth, int texHeight);
    void blit(Identifier var1, int var10, int var14, int i, int i1, int var12, int var16);
    void fill(int i, int i1, int i2, int i3, int shadowColor);
    void enableScissor(int i, int i1, int i2, int i3);
    void disableScissor();
    void drawString(Font var1, String var2, int var3, int var4, int var5, boolean var6);
    void drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5, boolean var6);
    void drawString(Font var1, Component var2, int var3, int var4, int var5);
    void drawString(Font var1, Component var2, int var3, int var4, int var5, boolean var6);
    void setColor(float v, float v1, float v2, float v3);
    void drawCenteredString(Font font, Component translatedKeyMessage, int i, int i1, int i2);
    void renderTooltip(Font font, MutableComponent translatable, int mouseX, int mouseY);
    void renderTooltip(Font font, List<Component> append, Optional<TooltipComponent> empty, int mouseX, int mouseY);
    int guiWidth();
    int guiHeight();

    void blit(AbstractTexture texture, int x1, int y1, int x2, int y2);

    void pushPose();
    void popPose();
    void translate(float x, float y, float z);
    void scale(float x, float y);

    void enableBlend();
    void disableBlend();

    default void text(Font f, String s, int x, int y, int color, boolean shadow) { drawString(f, s, x, y, color, shadow); }
    default void text(Font f, FormattedCharSequence s, int x, int y, int color, boolean shadow) { drawString(f, s, x, y, color, shadow); }
    default void text(Font f, Component c, int x, int y, int color) { drawString(f, c, x, y, color); }
    default void text(Font f, Component c, int x, int y, int color, boolean shadow) { drawString(f, c, x, y, color, shadow); }
    default void centeredText(Font f, Component c, int x, int y, int color) { drawCenteredString(f, c, x, y, color); }

//? if >=1.20 {
    static ISnGuiCanvas fromGuiParam(GuiGraphicsExtractor guiParam) {
//? } else {
    /*static ISnGuiCanvas fromGuiParam(GuiGraphics guiParam) {
*///? }
//? if >=1.21.6 {
        return SnGuiCanvas12106.withGuiParam(guiParam);
//? } else if >=1.20 {
        /*return SnGuiCanvas120.withGuiParam(guiParam);
*///? } else {
        /*return SnGuiCanvas119.withGuiParam(guiParam);
*///? }
    }
}
