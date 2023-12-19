package cn.zbx1425.worldcomment.gui;


import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.gui.components.AbstractWidget;
#if MC_VERSION >= "11700"
import net.minecraft.client.gui.narration.NarrationElementOutput;
#endif
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
#if MC_VERSION >= "12000"
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
#elif MC_VERSION >= "11904"
        public void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
#else
            public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
#endif
                if (!visible) return;
                String[] lines = this.getMessage().getString().split("\n");
                this.height = lines.length * 10;
                int textStart = Math.max(getY(), getY() + (getHeight() - 10 * lines.length) / 2);
                for (int i = 0; i < lines.length; ++i) {
                    int textWidth = Minecraft.getInstance().font.width(lines[i]);
#if MC_VERSION >= "11903"
                    int x = alignR ? this.padX() + this.padWidth() - textWidth : this.padX();
                    int y = textStart + 10 * i;
#else
                    int x = alignR ? this.padX() + this.width - textWidth : this.padX();
                    int y = textStart + 10 * i;
#endif
                    if (textWidth > this.padWidth()) {
                        int offset = (int)(System.currentTimeMillis() / 25 % (textWidth + 40));
#if MC_VERSION >= "12000"
                        guiGraphics.enableScissor(this.padX(), this #if MC_VERSION >= "11903" .getY() #else .y #endif, this.padX() + this.padWidth(), this #if MC_VERSION >= "11903" .getY() #else .y #endif + this.height);
                        guiGraphics.drawString(Minecraft.getInstance().font, lines[i], x - offset, y, -1);
                        guiGraphics.drawString(Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, -1);
                        guiGraphics.disableScissor();
#else
                        drawString(matrices, Minecraft.getInstance().font, lines[i], x - offset, y, -1);
                        drawString(matrices, Minecraft.getInstance().font, lines[i], x + textWidth + 40 - offset, y, -1);
                        RenderSystem.disableScissor();
#endif
                    } else {
#if MC_VERSION >= "12000"
                        guiGraphics.drawString(Minecraft.getInstance().font, lines[i], x, y, -1);
#else
                        drawString(matrices, Minecraft.getInstance().font, lines[i], x, y, -1);
#endif
                    }
                    if (!isActive()) {
#if MC_VERSION >= "12000"
                        guiGraphics.drawString(Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
#else
                        drawString(matrices, Minecraft.getInstance().font, "▶", x - 8, y, 0xffff0000);
#endif
                    }
                }
            }

            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
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