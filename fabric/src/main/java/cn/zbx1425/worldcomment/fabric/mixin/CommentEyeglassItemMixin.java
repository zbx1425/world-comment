package cn.zbx1425.worldcomment.fabric.mixin;

import cn.zbx1425.worldcomment.item.CommentEyeglassItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommentEyeglassItem.class)
public class CommentEyeglassItemMixin {

    /**
     * @author Zbx1425
     * @reason It's my class and I do whatever I want
     */
    @Overwrite
    public static Item.Properties injectFabricSettings(Item.Properties properties) {
        // TODO make it work on more versions
#if MC_VERSION >= "12100"
        return properties.equipmentSlot((livingEntity, itemStack) -> EquipmentSlot.HEAD);
#else
        return properties;
#endif
    }
}
