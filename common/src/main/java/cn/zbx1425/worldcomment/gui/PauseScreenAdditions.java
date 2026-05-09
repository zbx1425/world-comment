package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.joml.Vector2i;

import java.util.function.Consumer;

public class PauseScreenAdditions implements IGuiCommon {

    public static void init(Screen screen, Consumer<AbstractWidget> addWidget) {
        Vector2i widgetsTopLeft = new Vector2i(screen.width, screen.height), widgetsBottomRight = new Vector2i(0, 0);

        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            widgetsTopLeft.set(Math.min(widgetsTopLeft.x, widget.getX()), Math.min(widgetsTopLeft.y, widget.getY()));
            widgetsBottomRight.set(Math.max(widgetsBottomRight.x, widget.getX() + widget.getWidth()),
                Math.max(widgetsBottomRight.y, widget.getY() + widget.getHeight()));
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            int leftPos = ((AbstractContainerScreenAccessor)containerScreen).getLeftPos();
            int topPos = ((AbstractContainerScreenAccessor)containerScreen).getTopPos();
            for (Slot slot : containerScreen.getMenu().slots) {
                widgetsTopLeft.set(Math.min(widgetsTopLeft.x, leftPos + slot.x), Math.min(widgetsTopLeft.y, topPos + slot.y));
                widgetsBottomRight.set(Math.max(widgetsBottomRight.x, leftPos + slot.x + 20), Math.max(widgetsBottomRight.y, topPos + slot.y + 20));
            }
        }

        if (screen instanceof AbstractContainerScreen<?>) {
            addWidget.accept(new WidgetSnToggleButton(widgetsBottomRight.x() - 20, widgetsTopLeft.y() - 20));
        } else {
            addWidget.accept(new WidgetSnToggleButton(widgetsBottomRight.x() + 50, widgetsTopLeft.y()));
        }
    }
}
