package cn.zbx1425.worldcomment.platform.fabric;

//? if fabric {

/*import cn.zbx1425.worldcomment.platform.ServerPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class ServerPlatformImpl {

    public static boolean isFabric() {
        return true;
    }

    public static boolean isProduction() {
        return !FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static void registerPacket(Identifier resourceLocation) {
//? if >=1.21 {
        MainFabric.PACKET_REGISTRY.registerPacket(resourceLocation);
//? }
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, ServerPlatform.C2SPacketHandler packetCallback) {
//? if >=1.21 {
        MainFabric.PACKET_REGISTRY.registerNetworkReceiverC2S(resourceLocation, packetCallback);
//? } else {
        /^ServerPlayNetworking.registerGlobalReceiver(resourceLocation, (server, player, handler, packet, responseSender) -> packetCallback.handlePacket(server, player, packet));
^///? }
    }

    public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
            if (entity instanceof ServerPlayer) {
                consumer.accept((ServerPlayer) entity);
            }
        });
    }

    public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> consumer.accept(handler.player));
    }

    public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STARTING.register(consumer::accept);
    }

    public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STOPPING.register(consumer::accept);
    }

    public static void registerWorldSaveEvent(Consumer<MinecraftServer> consumer) {
//? if >=1.20.1 {
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> consumer.accept(server));
//? } else {
        /^ServerLifecycleEvents.SERVER_STOPPING.register(consumer::accept);
^///? }
    }

    public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
        ServerTickEvents.START_SERVER_TICK.register(consumer::accept);
    }

    public static void sendPacketToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
//? if >=1.21 {
        MainFabric.PACKET_REGISTRY.sendS2C(player, id, packet);
//? } else {
        /^ServerPlayNetworking.send(player, id, packet);
^///? }
    }
}

*///? }
