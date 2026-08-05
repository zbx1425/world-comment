package cn.zbx1425.worldcomment.gui;


#if MC_VERSION >= "12000"
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
#endif
import net.minecraft.client.gui.components.AbstractWidget;
#if MC_VERSION >= "11700"
import net.minecraft.client.gui.narration.NarrationElementOutput;
#endif
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
#if MC_VERSION >= "12000"
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
#elif MC_VERSION >= "11904"
        public void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
#else
            public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
#endif
                if (!visible) return;
                int textStart = Math.max(getY(), getY() + (getHeight() - 10 * lines.size()) / 2);
                for (int i = 0; i < lines.size(); ++i) {
                    int textWidth = Minecraft.getInstance().font.width(lines.get(i));
#if MC_VERSION >= "11903"
                    int x = alignR ? this.padX() + this.padWidth() - textWidth : this.padX();
                    int y = textStart + 10 * i;
#else
                    int x = alignR ? this.padX() + this.width - textWidth : this.padX();
                    int y = textStart + 10 * i;
#endif
#if MC_VERSION >= "12000"
                        guiGraphics.text(Minecraft.getInstance().font, lines.get(i), x, y, -1, false);
#else
                        drawString(matrices, Minecraft.getInstance().font, lines.get(i), x, y, -1);
#endif
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

#if MC_VERSION >= "11903"
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
#elif MC_VERSION >= "11700"
            @Override
            public void updateNarration(NarrationElementOutput arg) { }
#endif

#if MC_VERSION < "11903"
            protected int getX() {
                return x;
            }

            protected int getY() {
                return y;
            }
#endif


    private int padX() {
        return this #if MC_VERSION >= "11903" .getX() #else .x #endif + padding;
    }

    private int padWidth() {
        return this.getWidth() - padding * 2;
    }
}