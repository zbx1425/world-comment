package cn.zbx1425.worldcomment.platform.fabric;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
//? if >=1.20 {
import net.minecraft.core.registries.BuiltInRegistries;
//? }
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;


public interface RegistryUtilities {

    static Item.Properties createItemProperties() {
        return new Item.Properties();
    }

    static DefaultedRegistry<Item> registryGetItem() {
        //? if >=1.20 {
        return BuiltInRegistries.ITEM;
        //?} else {
        /*return Registry.ITEM;
        *///?}
    }

    static DefaultedRegistry<Block> registryGetBlock() {
        //? if >=1.20 {
        return BuiltInRegistries.BLOCK;
        //?} else {
        /*return Registry.BLOCK;
        *///?}
    }

    static Registry<BlockEntityType<?>> registryGetBlockEntityType() {
        //? if >=1.20 {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE;
        //?} else {
        /*return Registry.BLOCK_ENTITY_TYPE;
        *///?}
    }

    static DefaultedRegistry<EntityType<?>> registryGetEntityType() {
        //? if >=1.20 {
        return BuiltInRegistries.ENTITY_TYPE;
        //?} else {
        /*return Registry.ENTITY_TYPE;
        *///?}
    }

    static Registry<SoundEvent> registryGetSoundEvent() {
        //? if >=1.20 {
        return BuiltInRegistries.SOUND_EVENT;
        //?} else {
        /*return Registry.SOUND_EVENT;
        *///?}
    }

    static Registry<ParticleType<?>> registryGetParticleType() {
        //? if >=1.20 {
        return BuiltInRegistries.PARTICLE_TYPE;
        //?} else {
        /*return Registry.PARTICLE_TYPE;
        *///?}
    }
}