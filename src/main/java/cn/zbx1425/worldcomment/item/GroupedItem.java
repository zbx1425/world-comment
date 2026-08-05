package cn.zbx1425.worldcomment.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public interface GroupedItem {

    //? if >=1.20 {
    ResourceKey<CreativeModeTab> getTab();
    //?} else {
    /*CreativeModeTab getTab();
    *///?}

    //? if >=1.20 {
    static Item.Properties createProperties(Function<Item.Properties, Item.Properties> properties,
                Identifier id,
                Supplier<ResourceKey<CreativeModeTab>> getTab) {
    //?} else {
    /*static Item.Properties createProperties(Function<Item.Properties, Item.Properties> properties,
                Identifier id,
                Supplier<CreativeModeTab> getTab) {
    *///?}
        //? if >=1.21.2 {
        return properties.apply(new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
        );
        //?} else if >=1.20 {
        /*return properties.apply(new Item.Properties());
        *///?} else {
        /*return properties.apply(new Item.Properties()
                        .tab(getTab.get()));
        *///?}
    }
}
