package cn.zbx1425.worldcomment.gui;


import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? }
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.17 {
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? }
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class WidgetLabel extends AbstractWidget {

    public boolean alignR = false;

    public int padding = 0;

    private final Runnable onClick;

    public WidgetLabel(int x, int y, int width, int height, Component text) {
        super(x, y, width, height, text);
        this.onClick = null;
    }

    public WidgetLabel(int x, int y, int width, int height, Component text, Runnable onClick) {
        super(x, y, width, height, text);
        this.onClick = onClick;
    }

    @Override
//? if >=1.20 {
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
//? } else if >=1.19.4 {
        /*public void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
*///? } else {
            /*public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
*///? }
                if (!visible) return;
                String[] lines = this.getMessage().getString().split("\n");
                this.height = lines.length * 10;
                int textStart = Math.max(getY(), getY() + (getHeight() - 10 * lines.length) / 2);
                for (int i = 0; i < lines.length; ++i) {
                    int textWidth = Minecraft.getInstance().font.width(lines[i]);
//? if >=1.19.3 {
                    int x = alignR ? this.padX() + this.padWidth() - textWidth : this.padX();
                    int y = textStart + 10 * i;
//? } else {
                    /*int x = alignR ? this.padX() + this.width - textWidth : this.padX();
                    int y = textStart + 10 * i;
*///? }
                    if (textWidth > this.padWidth()) {
                        int offset = (int)(System.currentTimeMillis() / 25 % (textWidth + 40));
//? if >=1.20 {
                        guiGraphics.enableScissor(this.padX(), this #if MC_VERSION >= "11903" .getY() #else .y #endif, this.padX() + this.padWidth(), this #if MC_VERSION >= "11903" .getY() #else .y #endif + this.height);
                        guiGraphics.text(Minecraft.getInstance().font, lines[i], x - offset, y, -1);
                        guiGraphics.text(Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, -1);
                        guiGraphics.disableScissor();
//? } else {
                        /*drawString(matrices, Minecraft.getInstance().font, lines[i], x - offset, y, -1);
                        drawString(matrices, Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, -1);
                        RenderSystem.disableScissor();
*///? }
                    } else {
//? if >=1.20 {
                        guiGraphics.text(Minecraft.getInstance().font, lines[i], x, y, -1);
//? } else {
                        /*drawString(matrices, Minecraft.getInstance().font, lines[i], x, y, -1);
*///? }
                    }
                    if (!isActive()) {
//? if >=1.20 {
                        guiGraphics.text(Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
//? } else {
                        /*drawString(matrices, Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
*///? }
                    }
                }
            }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        if (onClick != null) onClick.run();
    }

//? if >=1.19.3 {
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
//? } else if >=1.17 {
            /*@Override
            public void updateNarration(NarrationElementOutput arg) { }
*///? }

//? if <1.19.3 {
            /*protected int getX() {
                return x;
            }

            protected int getY() {
                return y;
            }
*///? }


    private int padX() {
        return this #if MC_VERSION >= "11903" .getX() #else .x #endif + padding;
    }

    private int padWidth() {
        return this.getWidth() - padding * 2;
    }
}