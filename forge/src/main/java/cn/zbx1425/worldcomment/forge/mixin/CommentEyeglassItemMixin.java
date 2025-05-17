package cn.zbx1425.worldcomment.forge.mixin;

import cn.zbx1425.worldcomment.item.CommentEyeglassItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CommentEyeglassItem.class)
public class CommentEyeglassItemMixin implements IForgeItem {

    @Override
    public boolean isRepairable(ItemStack arg) {
        return false;
    }

    @Override
    public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }
}