package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void consumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Options options = Minecraft.getInstance().options;
        if ((Object)this == options.keyTogglePerspective) {
            if (CommentListScreen.handleKeyF5()) cir.setReturnValue(false);
        }
    }

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void matches(int keysym, int scancode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Options options = Minecraft.getInstance().options;
        if ((Object)this == options.keyScreenshot) {
            if (Screenshot.handleKeyF2()) cir.setReturnValue(false);
        }
    }
}
