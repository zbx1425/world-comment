package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.gui.PauseScreenAdditions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    protected InventoryScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        PauseScreenAdditions.init(this, this::addRenderableWidget);
    }
}
