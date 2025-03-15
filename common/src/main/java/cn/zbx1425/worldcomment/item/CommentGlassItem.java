package cn.zbx1425.worldcomment.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Function;
import java.util.function.Supplier;

public class CommentGlassItem extends GroupedItem {

    public CommentGlassItem() {
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
