package cn.zbx1425.worldcomment.neoforge.mixin;

import cn.zbx1425.worldcomment.item.CommentEyeglassItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CommentEyeglassItem.class)
public class CommentEyeglassItemMixin implements IItemExtension {

    @Override
    public boolean isRepairable(ItemStack arg) {
        return false;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        if (armorType == EquipmentSlot.HEAD) return true;
        return IItemExtension.super.canEquip(stack, armorType, entity);
    }
}
