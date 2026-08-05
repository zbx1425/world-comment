package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import net.minecraft.client.Minecraft;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;
//? if >=1.20.2
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.Button;
//? if >=1.21.6
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

//? if >=1.20
import java.util.function.Supplier;

public class WidgetColorButton extends Button implements IGuiCommon {

    int color;

    int iconU = -1, iconV = -1;

    public WidgetColorButton(int i, int j, int k, int l, Component component, int color, OnPress onPress) {
        //? if >=1.20 {
        super(i, j, k, l, component, onPress, Supplier::get);
        //?} else {
        /*super(i, j, k, l, component, onPress);
        *///?}
        this.color = color;
    }

    public WidgetColorButton(int k, int l, Component component, int color, OnPress onPress) {
        //? if >=1.20 {
        super(0, 0, k, l, component, onPress, Supplier::get);
        //?} else {
        /*super(0, 0, k, l, component, onPress);
        *///?}
        this.color = color;
    }

//? if >=1.21 {
private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("widget/button"), Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted"));
//? } else if >=1.20.2 {
    /*private static final WidgetSprites SPRITES = new WidgetSprites(new Identifier("widget/button"), new Identifier("widget/button_disabled"), new Identifier("widget/button_highlighted"));
*///? }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiCanvas guiGraphics = ISnGuiCanvas.fromGuiParam(guiParam);
        Minecraft minecraft = Minecraft.getInstance();
        if (this.active) {
            guiGraphics.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f, this.alpha);
        } else {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        }
        guiGraphics.enableBlend();
//? if >=1.21.6 {
        guiGraphics.getGuiParam().blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                this.active ? color : -1);
//? } else if >=1.20.2 {
        /*guiGraphics.getGuiParam().blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
*///? } else if >=1.19.3 {
        /*guiGraphics.blitNineSliced(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
*///? } else {
        /*guiGraphics.blitNineSliced(WIDGETS_LOCATION, this.x, this.y, this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
*///? }
        guiGraphics.disableBlend();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        int i = this.active ? 0xFFFFFF : 0xA0A0A0;
        if (iconU > -1) {
            guiParam.blit(RenderPipelines.GUI_TEXTURED, ATLAS_LOCATION, getX(), getY(), iconU, iconV,
                20, 20, 40, 40, ATLAS_SIZE, ATLAS_SIZE, i | 0xFF000000);
        }
        this.extractDefaultLabel(guiGraphics.getGuiParam().textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
//        this.renderString(guiGraphics.getGuiParam(), minecraft.font, i | Mth.ceil(this.alpha * 255.0f) << 24);
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

    public void useIcon(int iconU, int iconV) {
        this.iconU = iconU;
        this.iconV = iconV;
    }

//? if <1.20 {
    /*public void renderString(GuiGraphicsExtractor var1, net.minecraft.client.gui.Font var2, int var4) {
        int var3 = 2;
        int var5 = this.x + var3;
        int var6 = this.x + this.getWidth() - var3;
        renderScrollingString(var1, var2, this.getMessage(), var5, this.y, var6, this.y + this.getHeight(), var4);
    }

    @Override
    public Component getMessage() {
        return Component.empty();
    }
*///? }
}
