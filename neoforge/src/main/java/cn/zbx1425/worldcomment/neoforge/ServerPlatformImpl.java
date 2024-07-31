package cn.zbx1425.worldcomment.neoforge;

import cn.zbx1425.worldcomment.ServerPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public class ServerPlatformImpl {

    public static boolean isFabric() {
        return false;
    }

    public static void registerPacket(ResourceLocation resourceLocation) {
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.registerPacket(resourceLocation);
#endif
    }

    public static void registerNetworkReceiver(ResourceLocation resourceLocation, ServerPlatform.C2SPacketHandler packetCallback) {
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.registerNetworkReceiverC2S(resourceLocation, packetCallback);
#else
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, resourceLocation, (packet, context) -> {
            final Player player = context.getPlayer();
            if (player != null) {
                packetCallback.handlePacket(player.getServer(), (ServerPlayer) player, packet);
            }
        });
#endif
    }

    public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
        RegistryUtilities.registerPlayerJoinEvent(consumer);
        RegistryUtilities.registerPlayerChangeDimensionEvent(consumer);
    }

    public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
        RegistryUtilities.registerPlayerQuitEvent(consumer);
    }

    public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
        RegistryUtilities.registerServerStartingEvent(consumer);
    }

    public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
        RegistryUtilities.registerServerStoppingEvent(consumer);
    }

    public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
        RegistryUtilities.registerTickEvent(consumer);
    }

    public static void sendPacketToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf packet) {
        packet.resetReaderIndex();
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.sendS2C(player, id, packet);
#else
        NetworkManager.sendToPlayer(player, id, packet);
#endif
    }
}