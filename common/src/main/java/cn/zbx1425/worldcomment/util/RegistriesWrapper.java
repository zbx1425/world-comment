package cn.zbx1425.worldcomment.util;

import cn.zbx1425.worldcomment.item.GroupedItem;
import net.minecraft.client.KeyMapping;
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

    void registerBlockAndItem(String id, RegistryObject<Block> block, #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif tab);

    void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType);

    void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType);

    void registerSoundEvent(String id, SoundEvent soundEvent);

}
