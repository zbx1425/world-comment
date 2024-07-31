package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.ServerPlatform;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class ServerPlatformImpl {

    public static boolean isFabric() {
        return false;
    }

    public static void registerPacket(ResourceLocation resourceLocation) {

    }

    public static void registerNetworkReceiver(ResourceLocation resourceLocation, ServerPlatform.C2SPacketHandler packetCallback) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, resourceLocation, (packet, context) -> {
            final Player player = context.getPlayer();
            if (player != null) {
                packetCallback.handlePacket(player.getServer(), (ServerPlayer) player, packet);
            }
        });
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
        NetworkManager.sendToPlayer(player, id, packet);
    }
}