package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PauseScreenAdditions implements IGuiCommon {

    private static WidgetColorButton btnOn, btnOff;

    public static void init(Consumer<AbstractWidget> addWidget) {
        btnOn = new WidgetColorButton(30, 10, 20, 20,
                Component.translatable("options.on"), 0xFFC5E1A5, sender -> {
            btnOn.active = false;
            btnOff.active = true;
            MainClient.CLIENT_CONFIG.commentVisibilityPreference = true;
        });
        btnOff = new WidgetColorButton(50, 10, 20, 20,
                Component.translatable("options.off"), 0xFF81D4FA, sender -> {
            btnOn.active = true;
            btnOff.active = false;
            MainClient.CLIENT_CONFIG.commentVisibilityPreference = false;
        });
        btnOn.active = !MainClient.CLIENT_CONFIG.commentVisibilityPreference;
        btnOff.active = MainClient.CLIENT_CONFIG.commentVisibilityPreference;
        addWidget.accept(btnOn);
        addWidget.accept(btnOff);
    }

    public static void render(ISnGuiGraphics guiGraphics) {
        guiGraphics.blit(ATLAS_LOCATION, 10, 10, 20, 20,
                176, 96, 32, 32, 256, 256);
    }
}
