package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    void swapPaint(double direction, CallbackInfo ci) {
        int pickedCommentsSize = ClientRayPicking.pickedComments.size();
        if (pickedCommentsSize > 1) {
            int dir = -(int)Math.signum(direction);
            ClientRayPicking.overlayOffset = Mth.clamp(ClientRayPicking.overlayOffset + dir, 0, pickedCommentsSize - 1);
            ci.cancel();
        }
    }
}
