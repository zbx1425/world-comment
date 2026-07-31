package cn.zbx1425.worldcomment;


import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class ServerPlatform {

    @ExpectPlatform
    public static boolean isFabric() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    public static <T> DataComponentType<T> createDataComponentType(Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return (DataComponentType<T>) DataComponentType.builder().persistent((Codec<Object>)codec)
            .networkSynchronized((StreamCodec<? super RegistryFriendlyByteBuf, Object>)streamCodec).build();
    }

    @ExpectPlatform
    public static void registerPacket(Identifier resourceLocation) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerNetworkReceiver(Identifier resourceLocation, C2SPacketHandler packetCallback) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerWorldSaveEvent(Consumer<MinecraftServer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendPacketToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
        throw new AssertionError();
    }

    @FunctionalInterface
    public interface C2SPacketHandler {

        void handlePacket(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet);
    }
}
