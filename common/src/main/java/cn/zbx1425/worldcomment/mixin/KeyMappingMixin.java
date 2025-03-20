package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.interop.AccessoriesInterop;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.item.ItemStack;
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
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            if (CommentToolItem.Client.getHoldingCommentTool() != null) {
                CommentListScreen.triggerOpen();
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void matches(int keysym, int scancode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Options options = Minecraft.getInstance().options;
        if ((Object)this == options.keyScreenshot) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
            if (item != null && CommentToolItem.getUploadJobId(item) == null) {
                Screenshot.triggerCommentSend(true);
                cir.setReturnValue(false);
                return;
            }

            if (AccessoriesInterop.isWearingEyeglass()) {
                Screenshot.triggerCommentSend(false);
                cir.setReturnValue(false);
            }
        }
    }
}
