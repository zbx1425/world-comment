package cn.zbx1425.worldcomment.fabric;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
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
        return BuiltInRegistries.ITEM;
    }

    static DefaultedRegistry<Block> registryGetBlock() {
        return BuiltInRegistries.BLOCK;
    }

    static Registry<BlockEntityType<?>> registryGetBlockEntityType() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE;
    }

    static DefaultedRegistry<EntityType<?>> registryGetEntityType() {
        return BuiltInRegistries.ENTITY_TYPE;
    }

    static Registry<SoundEvent> registryGetSoundEvent() {
        return BuiltInRegistries.SOUND_EVENT;
    }

    static Registry<ParticleType<?>> registryGetParticleType() {
        return BuiltInRegistries.PARTICLE_TYPE;
    }
}