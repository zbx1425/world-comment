package cn.zbx1425.worldcomment.item;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public interface GroupedItem {

    #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTab();

    static Item.Properties createProperties(Function<Item.Properties, Item.Properties> properties) {
        #if MC_VERSION >= "12000"
            return properties.apply(new Item.Properties());
        #else
            return properties.apply(new Item.Properties().tab(getTab()));
        #endif
    }
}
