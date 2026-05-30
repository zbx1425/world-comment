package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.gui.WidgetSnToggleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        WidgetSnToggleButton btnToggleSn = new WidgetSnToggleButton(
            leftPos + imageWidth - (ServerPlatform.isFabric() ? 24 : 44),
            topPos - (ServerPlatform.isFabric() ? 48 : 50),
            true);
        addRenderableWidget(btnToggleSn);
    }
}
