package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.render.CommentOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void render(GuiGraphics guiGraphics, float partialTick, CallbackInfo callbackInfo) {
        if (minecraft.options.getCameraType().isFirstPerson()) {
            CommentOverlayRenderer.render(guiGraphics, partialTick);
        }
    }
}
