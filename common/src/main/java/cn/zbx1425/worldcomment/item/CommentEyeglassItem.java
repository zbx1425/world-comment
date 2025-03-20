package cn.zbx1425.worldcomment.item;

#if MC_VERSION >= "12000" import net.minecraft.core.registries.Registries; #endif
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class CommentEyeglassItem extends GroupedItem {

    public CommentEyeglassItem() {
        super(
                () -> #if MC_VERSION >= "12000"
                ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace("tools_and_utilities"))
            #else
        CreativeModeTab.TAB_MISC
            #endif,
                properties -> properties.stacksTo(1)
        );
    }
}
