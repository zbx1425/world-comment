package cn.zbx1425.worldcomment.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public class PauseScreenAdditions implements IGuiCommon {

    public static void init(Screen screen, Consumer<AbstractWidget> addWidget) {
        addWidget.accept(new WidgetSnToggleButton(
                screen.width - WidgetSnToggleButton.BTN_SIZE - 20,
                screen.height - WidgetSnToggleButton.BTN_SIZE - 20));
    }
}
