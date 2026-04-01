package cn.zbx1425.worldcomment.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class ClientPlatformImpl {

    public static void registerKeyBinding(KeyMapping keyMapping) {
        KeyMappingHelper.registerKeyMapping(keyMapping);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
#if MC_VERSION >= "12100"
        MainFabric.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
#else
        ClientPlayNetworking.registerGlobalReceiver(resourceLocation, (client, handler, packet, responseSender) -> consumer.accept(packet));
#endif
    }

    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        ClientEntityEvents.ENTITY_LOAD.register((entity, clientWorld) -> {
            if (entity == Minecraft.getInstance().player) {
                consumer.accept((LocalPlayer) entity);
            }
        });
    }

    public static void registerPlayerLeaveEvent(Runnable runnable) {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
           runnable.run();
        });
    }

    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        ClientTickEvents.START_CLIENT_TICK.register(consumer::accept);
    }

    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
#if MC_VERSION >= "12100"
        MainFabric.PACKET_REGISTRY.sendC2S(id, packet);
#else
        ClientPlayNetworking.send(id, packet);
#endif
    }
}