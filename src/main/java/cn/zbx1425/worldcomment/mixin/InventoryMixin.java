package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if <1.21.8 {
/*@Mixin(Inventory.class)
*///? } else {
@Mixin(MouseHandler.class)
//? }
public class InventoryMixin {

//? if <1.21.8 {
    /*@Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    void swapPaint(double direction, CallbackInfo ci) {
        int pickedCommentsSize = ClientRayPicking.pickedComments.size();
        if (pickedCommentsSize > 1) {
            int dir = -(int)Math.signum(direction);
            ClientRayPicking.overlayOffset = Mth.clamp(ClientRayPicking.overlayOffset + dir, 0, pickedCommentsSize - 1);
            ci.cancel();
        }
    }
*///? } else {
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
    int onScroll(double yOffset, int selected, int selectionSize, Operation<Integer> original) {
        int pickedCommentsSize = ClientRayPicking.pickedComments.size();
        if (pickedCommentsSize > 1) {
            int dir = -(int)Math.signum(yOffset);
            ClientRayPicking.overlayOffset = Mth.clamp(ClientRayPicking.overlayOffset + dir, 0, pickedCommentsSize - 1);
            return selected;
        }
        return original.call(yOffset, selected, selectionSize);
    }
//? }
}


