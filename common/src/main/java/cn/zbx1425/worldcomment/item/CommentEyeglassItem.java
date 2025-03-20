package cn.zbx1425.worldcomment.item;

#if MC_VERSION >= "12000" import cn.zbx1425.worldcomment.Main;
import net.minecraft.core.registries.Registries; #endif
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class CommentEyeglassItem extends ArmorItem implements GroupedItem {

    public CommentEyeglassItem() {
        super(
            ArmorMaterials.LEATHER,
            Type.HELMET,
            GroupedItem.createProperties(properties ->
                properties.stacksTo(1)
            )
        );
    }

    @Override
    public ResourceKey<CreativeModeTab> getTab() {
        #if MC_VERSION >= "12000"
            return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Main.vanillaId("tools_and_utilities"));
        #else
            return CreativeModeTab.TAB_MISC;
        #endif
    }
}
