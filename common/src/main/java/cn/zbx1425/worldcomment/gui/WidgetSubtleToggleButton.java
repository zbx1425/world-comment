package cn.zbx1425.worldcomment.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class WidgetSubtleToggleButton extends AbstractWidget implements IGuiCommon {

    int iconU, iconV;

    boolean checked;
    BooleanConsumer onChange;
    Component unCheckedDescription, checkedDescription;

    public WidgetSubtleToggleButton(int iconU, int iconV,
                                    @NonNull Component unCheckedDescription, @NonNull Component checkedDescription,
                                    boolean checked, @Nullable BooleanConsumer onChange) {
        super(0, 0, 20, 20, CommonComponents.EMPTY);
        this.iconU = iconU;
        this.iconV = iconV;
        this.checked = checked;
        this.onChange = onChange;
        this.unCheckedDescription = unCheckedDescription;
        this.checkedDescription = checkedDescription;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        checked = !checked;
        if (onChange != null) onChange.accept(checked);
        super.onClick(event, doubleClick);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (isHovered()) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFFE0E0E0);

            Component unCheckedLine = checked
                ? unCheckedDescription.copy().withStyle(ChatFormatting.GRAY)
                : Component.literal("> ").withStyle(ChatFormatting.GOLD).append(unCheckedDescription.copy()
                        .withStyle(Style.EMPTY.withBold(true).withColor(0xFFFFFFFF)));
            Component checkedLine = !checked
                ? checkedDescription.copy().withStyle(ChatFormatting.GRAY)
                : Component.literal("> ").withStyle(ChatFormatting.GOLD).append(checkedDescription.copy()
                        .withStyle(Style.EMPTY.withBold(true).withColor(0xFFFFFFFF)));
            graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(unCheckedLine, checkedLine), mouseX, mouseY);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS_LOCATION,
            getX(), getY(), checked ? iconU + 40 : iconU, iconV, 20, 20, 40, 40, ATLAS_SIZE, ATLAS_SIZE);
    }

    public boolean selected() {
        return checked;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}
