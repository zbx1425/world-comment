package cn.zbx1425.worldcomment.util;

import cn.zbx1425.worldcomment.item.GroupedItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface RegistriesWrapper {

    void registerBlock(String id, RegistryObject<Block> block);

    <T extends Item & GroupedItem> void registerItem(String id, RegistryObject<T> item);

    //? if >=1.20 {
    void registerBlockAndItem(String id, RegistryObject<Block> block, ResourceKey<CreativeModeTab> tab);
    //?} else {
    /*void registerBlockAndItem(String id, RegistryObject<Block> block, CreativeModeTab tab);
    *///?}

    void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType);

    void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType);

    void registerSoundEvent(String id, SoundEvent soundEvent);

    <T> void registerDataComponentType(String id, RegistryObject<DataComponentType<T>> componentType);

}
