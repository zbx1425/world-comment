package cn.zbx1425.worldcomment.fabric;


import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RegistriesWrapperImpl implements RegistriesWrapper {

    @Override
    public void registerBlock(String id, RegistryObject<Block> block) {
        Registry.register(RegistryUtilities.registryGetBlock(), new ResourceLocation(Main.MOD_ID, id), block.get());
    }

    @Override
    public void registerBlockAndItem(String id, RegistryObject<Block> block, ResourceKey<CreativeModeTab> tab) {
        Registry.register(RegistryUtilities.registryGetBlock(), new ResourceLocation(Main.MOD_ID, id), block.get());
        final BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties());
        Registry.register(RegistryUtilities.registryGetItem(), new ResourceLocation(Main.MOD_ID, id), blockItem);
        ItemGroupEvents.modifyEntriesEvent(tab).register(consumer -> consumer.accept(blockItem));
    }

    @Override
    public void registerItem(String id, RegistryObject<Item> item, ResourceKey<CreativeModeTab> tab) {
        Registry.register(RegistryUtilities.registryGetItem(), new ResourceLocation(Main.MOD_ID, id), item.get());
        ItemGroupEvents.modifyEntriesEvent(tab).register(consumer -> consumer.accept(item.get()));
    }

    @Override
    public void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType) {
        Registry.register(RegistryUtilities.registryGetBlockEntityType(), new ResourceLocation(Main.MOD_ID, id), blockEntityType.get());
    }

    @Override
    public void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        Registry.register(RegistryUtilities.registryGetEntityType(), new ResourceLocation(Main.MOD_ID, id), entityType.get());
    }

    @Override
    public void registerSoundEvent(String id, SoundEvent soundEvent) {
        Registry.register(RegistryUtilities.registryGetSoundEvent(), new ResourceLocation(Main.MOD_ID, id), soundEvent);
    }
}