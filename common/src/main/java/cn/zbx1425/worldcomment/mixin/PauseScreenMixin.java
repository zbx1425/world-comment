package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.gui.PauseScreenAdditions;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
        PauseScreenAdditions.init(this::addRenderableWidget);
    }

    @Inject(method = "render", at = @At("RETURN"))
    void onRender(GuiGraphics guiParam, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        PauseScreenAdditions.render(guiGraphics);
    }
}
