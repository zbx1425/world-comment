package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
#if MC_VERSION >= "12000" import net.minecraft.core.registries.Registries; #endif
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CommentEyeglassItem extends Item implements GroupedItem #if MC_VERSION >= "11904", Equipable #endif {

    public CommentEyeglassItem() {
        super(
            GroupedItem.createProperties(properties -> injectFabricSettings(
                    properties.stacksTo(1)),
                    CommentEyeglassItem::getTabImpl)
        );
    }

    public static Properties injectFabricSettings(Properties properties) {
        return properties;
    }

    @Override
    public #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTab() {
        return getTabImpl();
    }

    public static #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTabImpl() {
        #if MC_VERSION >= "12000"
            return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Main.vanillaId("tools_and_utilities"));
        #else
            return CreativeModeTab.TAB_MISC;
        #endif
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
#if MC_VERSION >= "11904"
        return swapWithEquipmentSlot(this, level, player, usedHand);
#else
        ItemStack handItem = player.getItemInHand(usedHand);
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(handItem);
        ItemStack targetItem = player.getItemBySlot(slot);
        if (targetItem.isEmpty()) {
            player.setItemSlot(slot, handItem.copy());
            if (!level.isClientSide()) {
                player.awardStat(Stats.ITEM_USED.get(this));
            }
            handItem.setCount(0);
            return InteractionResultHolder.sidedSuccess(handItem, level.isClientSide());
        } else {
            return InteractionResultHolder.fail(handItem);
        }
#endif
    }

#if MC_VERSION >= "11904"
    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
#endif
}
