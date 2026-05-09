package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.item.PlaceableCommentItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
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

            if (CommentToolItem.Client.getHolding() != null) {
                CommentListScreen.triggerOpen();
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void matches(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Options options = Minecraft.getInstance().options;
        boolean isScreenshotHotkey = CommentToolItem.Client.getSendHotkeyIsModifier()
            ? ((Object)this == options.keyScreenshot && MainClient.KEY_SEND_COMMENT_MODIFIER.get().isDown())
            : ((Object)this == MainClient.KEY_SEND_COMMENT_MODIFIER.get());
        if (isScreenshotHotkey) {
            if (CommentToolItem.Client.handleScreenshotKey()) {
                cir.setReturnValue(false);
            }
        }
    }
}
