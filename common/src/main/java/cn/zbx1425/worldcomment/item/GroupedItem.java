package cn.zbx1425.worldcomment.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public interface GroupedItem {

    #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTab();

    static Item.Properties createProperties(Function<Item.Properties, Item.Properties> properties,
                Identifier id,
                Supplier<#if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif> getTab) {
        return properties.apply(new Item.Properties()
        #if MC_VERSION < "12000"
                        .tab(getTab.get()));
        #endif
        #if MC_VERSION >= "12102"
                        .setId(ResourceKey.create(Registries.ITEM, id))
        #endif
        );
    }
}
