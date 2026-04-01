package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class WidgetSnToggleButton extends AbstractWidget implements IGuiCommon {

    public static final int BTN_SIZE = 20;

    public WidgetSnToggleButton(int x, int y) {
        super(x, y, BTN_SIZE, BTN_SIZE, Component.empty());
    }

    @Override
#if MC_VERSION >= "12000"
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
#else
    public void render(PoseStack guiParam, int mouseX, int mouseY, float partialTick) {
        super.render(guiParam, mouseX, mouseY, partialTick);
#endif
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        guiGraphics.blit(ATLAS_LOCATION, getX(), getY(),
                BTN_SIZE, BTN_SIZE,
                160 + (MainClient.CLIENT_CONFIG.perServerPreference.commentVisibilityPreference ? 0 : 32), 96,
                32, 32, 256, 256);
        if (mouseX > getX() && mouseX < getX() + BTN_SIZE &&
                mouseY > getY() && mouseY < getY() + BTN_SIZE) {
            guiGraphics.blit(ATLAS_LOCATION, getX(), getY(),
                    BTN_SIZE, BTN_SIZE,
                    160 + 64, 96,
                    32, 32, 256, 256);
            guiGraphics.renderTooltip(Minecraft.getInstance().font, List.of(
                    Component.translatable("gui.worldcomment.toggler.status",
                            MainClient.CLIENT_CONFIG.perServerPreference.commentVisibilityPreference ?
                                    Component.translatable("gui.worldcomment.toggler.enabled") :
                                    Component.translatable("gui.worldcomment.toggler.disabled")),
                    Component.translatable("gui.worldcomment.toggler.toggle"),
                    Component.translatable("gui.worldcomment.toggler.create",
                            Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.BLUE)),
                    Component.translatable("gui.worldcomment.toggler.nearby",
                            Component.literal(String.valueOf(ClientRayPicking.nearbyCommentsCount)).withStyle(ChatFormatting.GOLD)),
                    Component.translatable("gui.worldcomment.toggler.manage")
            ), Optional.empty(), mouseX, mouseY);
        }

        Component countComponent = Component.literal(String.format("%dx", ClientRayPicking.nearbyCommentsCount));
        int countWidth = Minecraft.getInstance().font.width(countComponent);
        int yOffset = (BTN_SIZE - Minecraft.getInstance().font.lineHeight) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, countComponent,
                getX() - BTN_SIZE / 3 - countWidth, getY() + yOffset,
                0xFFFFFFFF, true);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        MainClient.CLIENT_CONFIG.perServerPreference.commentVisibilityPreference = !MainClient.CLIENT_CONFIG.perServerPreference.commentVisibilityPreference;
        MainClient.CLIENT_CONFIG.perServerPreference.isDirty = true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
