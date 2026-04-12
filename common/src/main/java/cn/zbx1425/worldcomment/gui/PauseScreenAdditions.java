package cn.zbx1425.worldcomment.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
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

        addWidget.accept(new WidgetSnToggleButton(
                widgetsBottomRight.x() + 20,
                widgetsTopLeft.y() + 40
        ));
    }
}
