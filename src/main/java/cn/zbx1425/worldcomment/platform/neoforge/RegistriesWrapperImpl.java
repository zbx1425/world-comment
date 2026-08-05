package cn.zbx1425.worldcomment.platform.neoforge;

//? if neoforge {

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.item.GroupedItem;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.21 {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
//? } else {
/*//? if >=1.20
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
*///? }

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistriesWrapperImpl implements RegistriesWrapper {

//? if >=1.21 {
private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Main.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Main.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Main.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Main.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Main.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Main.MOD_ID);
//? } else {
    /*private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Main.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Main.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);
*///? }

    @Override
    public void registerBlock(String id, RegistryObject<Block> block) {
        BLOCKS.register(id, block::get);
    }

    @Override
    //? if >=1.20 {
    public void registerBlockAndItem(String id, RegistryObject<Block> block, ResourceKey<CreativeModeTab> tab) {
    //?} else {
    /*public void registerBlockAndItem(String id, RegistryObject<Block> block, CreativeModeTab tab) {
    *///?}
        BLOCKS.register(id, block::get);
        ITEMS.register(id, () -> {
            final BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties());
            registerCreativeModeTab(tab, blockItem);
            return blockItem;
        });
    }

    @Override
    public <T extends Item & GroupedItem> void registerItem(String id, RegistryObject<T> item) {
        ITEMS.register(id, () -> {
            final T itemObject = item.get();
            registerCreativeModeTab(itemObject.getTab(), itemObject);
            return itemObject;
        });
    }

    @Override
    public void registerBlockEntityType(String id, RegistryObject<? extends BlockEntityType<? extends BlockEntity>> blockEntityType) {
        BLOCK_ENTITY_TYPES.register(id, blockEntityType::get);
    }

    @Override
    public void registerEntityType(String id, RegistryObject<? extends EntityType<? extends Entity>> entityType) {
        ENTITY_TYPES.register(id, entityType::get);
    }

    @Override
    public void registerSoundEvent(String id, SoundEvent soundEvent) {
        SOUND_EVENTS.register(id, () -> soundEvent);
    }


    @Override
    public <T> void registerDataComponentType(String id, RegistryObject<DataComponentType<T>> componentType) {
        DATA_COMPONENT_TYPES.register(id, componentType::get);
    }


    public final List<KeyMapping> keyMappings = new ArrayList<>();

    public void registerAllDeferred(IEventBus eventBus) {
        ITEMS.register(eventBus);
        BLOCKS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        SOUND_EVENTS.register(eventBus);
        DATA_COMPONENT_TYPES.register(eventBus);
    }


    //? if >=1.20 {
    private static final Map<ResourceKey<CreativeModeTab>, ArrayList<Item>> CREATIVE_TABS = new HashMap<>();
    //?} else {
    /*private static final Map<CreativeModeTab, ArrayList<Item>> CREATIVE_TABS = new HashMap<>();
    *///?}

    //? if >=1.20 {
    public static void registerCreativeModeTab(ResourceKey<CreativeModeTab> resourceLocation, Item item) {
    //?} else {
    /*public static void registerCreativeModeTab(CreativeModeTab resourceLocation, Item item) {
    *///?}
        if (resourceLocation == null) return;
        CREATIVE_TABS.computeIfAbsent(resourceLocation, ignored -> new ArrayList<>()).add(item);
    }

    public static class RegisterCreativeTabs {

//? if >=1.20 {
        @SubscribeEvent
        public static void onRegisterCreativeModeTabsEvent(BuildCreativeModeTabContentsEvent event) {
            CREATIVE_TABS.forEach((key, items) -> {
                if (event.getTabKey().equals(key)) {
                    //? if >=1.21 {
                    items.forEach(item -> event.accept(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                    //?} else {
                    /*items.forEach(item -> event.getEntries().put(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                    *///?}
                }
            });
        }
//? }

    }
}

//? }
