package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.util.FrameTask;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    void render(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        FrameTask.onFrameFinished();
    }
}
