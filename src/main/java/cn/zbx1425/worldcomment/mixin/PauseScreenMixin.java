package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.gui.WidgetSnToggleButton;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphicsExtractor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        Vector2i widgetsTopLeft = new Vector2i(width, height), widgetsBottomRight = new Vector2i(0, 0);

        for (GuiEventListener child : children()) {
            if (!(child instanceof Button widget)) continue;
            widgetsTopLeft.set(Math.min(widgetsTopLeft.x, widget.getX()), Math.min(widgetsTopLeft.y, widget.getY()));
            widgetsBottomRight.set(Math.max(widgetsBottomRight.x, widget.getX() + widget.getWidth()),
                Math.max(widgetsBottomRight.y, widget.getY() + widget.getHeight()));
        }

        addRenderableWidget(new WidgetSnToggleButton(widgetsBottomRight.x() + 10, widgetsTopLeft.y(), false));
    }
}
