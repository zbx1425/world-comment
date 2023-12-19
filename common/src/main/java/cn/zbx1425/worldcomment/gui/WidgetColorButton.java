package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; import com.mojang.blaze3d.vertex.PoseStack; #endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

#if MC_VERSION >= "12000" import java.util.function.Supplier; #endif

public class WidgetColorButton extends Button implements IGuiCommon {

    int color;

    public WidgetColorButton(int i, int j, int k, int l, Component component, int color, OnPress onPress) {
        super(i, j, k, l, component, onPress #if MC_VERSION >= "12000" , Supplier::get #endif);
        this.color = color;
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
        Minecraft minecraft = Minecraft.getInstance();
        if (this.active) {
            guiGraphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f, this.alpha);
        } else {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        }
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.blitNineSliced(WIDGETS_LOCATION, this #if MC_VERSION >= "11903" .getX() #else .x #endif, this #if MC_VERSION >= "11903" .getY() #else .y #endif, this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = this.active ? 0xFFFFFF : 0xA0A0A0;
        this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0f) << 24);
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }
        return 46 + i * 20;
    }

#if MC_VERSION < "12000"
    public void renderString(GuiGraphics var1, net.minecraft.client.gui.Font var2, int var4) {
        int var3 = 2;
        int var5 = this.x + var3;
        int var6 = this.x + this.getWidth() - var3;
        renderScrollingString(var1, var2, this.getMessage(), var5, this.y, var6, this.y + this.getHeight(), var4);
    }
#endif
}
