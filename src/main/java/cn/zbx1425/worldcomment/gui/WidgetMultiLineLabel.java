package cn.zbx1425.worldcomment.gui;


//? if >=1.20 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? }
import net.minecraft.client.gui.components.AbstractWidget;
//? if >=1.17 {
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? }
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class WidgetMultiLineLabel extends AbstractWidget {

    public boolean alignR = false;

    public int padding = 0;

    private List<FormattedCharSequence> lines = List.of();

    private final Runnable onClick;

    public WidgetMultiLineLabel(int x, int y, int width, int height, Component text) {
        super(x, y, width, height, text);
        this.onClick = null;
    }

    public WidgetMultiLineLabel(int x, int y, int width, int height, Component text, Runnable onClick) {
        super(x, y, width, height, text);
        this.onClick = onClick;
    }

    public void repositionEntries() {
        this.lines = Minecraft.getInstance().font.split(message, width);
        this.height = lines.size() * 10;
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
                int textStart = Math.max(getY(), getY() + (getHeight() - 10 * lines.size()) / 2);
                for (int i = 0; i < lines.size(); ++i) {
                    int textWidth = Minecraft.getInstance().font.width(lines.get(i));
//? if >=1.19.3 {
                    int x = alignR ? this.padX() + this.padWidth() - textWidth : this.padX();
                    int y = textStart + 10 * i;
//? } else {
                    /*int x = alignR ? this.padX() + this.width - textWidth : this.padX();
                    int y = textStart + 10 * i;
*///? }
//? if >=1.20 {
                        guiGraphics.text(Minecraft.getInstance().font, lines.get(i), x, y, -1, false);
//? } else {
                        /*drawString(matrices, Minecraft.getInstance().font, lines.get(i), x, y, -1);
*///? }
                }
            }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (onClick == null) return false;
        return super.mouseClicked(event, doubleClick);
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