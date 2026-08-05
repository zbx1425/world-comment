package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.gui.WidgetSnToggleButton;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {

    public InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component title) {
        super(menu, recipeBookComponent, inventory, title);
    }

    @Unique
    private WidgetSnToggleButton worldcomment$btnToggleSn;

    @Inject(method = "init", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        worldcomment$btnToggleSn = new WidgetSnToggleButton(leftPos + imageWidth - 26, topPos - 24, true);
        addRenderableWidget(worldcomment$btnToggleSn);
    }

    @Inject(method = "onRecipeBookButtonClick", at = @At("HEAD"))
    protected void onRecipeBookButtonClick(CallbackInfo ci) {
        worldcomment$btnToggleSn.setPosition(leftPos + imageWidth - 30, topPos - 24);
    }
}
