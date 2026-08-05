package cn.zbx1425.worldcomment.platform;


import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

//? if neoforge
import cn.zbx1425.worldcomment.platform.neoforge.ServerPlatformImpl;
//? if fabric
//import cn.zbx1425.worldcomment.platform.fabric.ServerPlatformImpl;

public class ServerPlatform {

    public static boolean isFabric() {
        //? if fabric
        //return true;
        //? if !fabric
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T> DataComponentType<T> createDataComponentType(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return (DataComponentType<T>) DataComponentType.builder().persistent((Codec<Object>)codec)
            .networkSynchronized((StreamCodec<? super RegistryFriendlyByteBuf, Object>)streamCodec).build();
    }

    public static void registerPacket(Identifier resourceLocation) {
        ServerPlatformImpl.registerPacket(resourceLocation);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, C2SPacketHandler packetCallback) {
        ServerPlatformImpl.registerNetworkReceiver(resourceLocation, packetCallback);
    }

    public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
        ServerPlatformImpl.registerPlayerJoinEvent(consumer);
    }

    public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
        ServerPlatformImpl.registerPlayerQuitEvent(consumer);
    }

    public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
        ServerPlatformImpl.registerServerStartingEvent(consumer);
    }

    public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
        ServerPlatformImpl.registerServerStoppingEvent(consumer);
    }

    public static void registerWorldSaveEvent(Consumer<MinecraftServer> consumer) {
        ServerPlatformImpl.registerWorldSaveEvent(consumer);
    }

    public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
        ServerPlatformImpl.registerTickEvent(consumer);
    }

    public static void sendPacketToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
        ServerPlatformImpl.sendPacketToPlayer(player, id, packet);
    }

    @FunctionalInterface
    public interface C2SPacketHandler {

        void handlePacket(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet);
    }
}
