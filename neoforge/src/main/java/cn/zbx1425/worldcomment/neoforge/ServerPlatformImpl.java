package cn.zbx1425.worldcomment.neoforge;

import cn.zbx1425.worldcomment.ServerPlatform;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.function.Consumer;

public class ServerPlatformImpl {

    public static boolean isFabric() {
        return false;
    }

    public static void registerPacket(Identifier resourceLocation) {
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.registerPacket(resourceLocation);
#endif
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, ServerPlatform.C2SPacketHandler packetCallback) {
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

    public static List<Consumer<ServerPlayer>> PLAYER_JOIN_EVENT = new ObjectArrayList<>();
    public static void registerPlayerJoinEvent(Consumer<ServerPlayer> consumer) {
        PLAYER_JOIN_EVENT.add(consumer);
    }

    public static List<Consumer<ServerPlayer>> PLAYER_QUIT_EVENT = new ObjectArrayList<>();
    public static void registerPlayerQuitEvent(Consumer<ServerPlayer> consumer) {
        PLAYER_QUIT_EVENT.add(consumer);
    }

    public static List<Consumer<MinecraftServer>> SERVER_STARTING_EVENT = new ObjectArrayList<>();
    public static void registerServerStartingEvent(Consumer<MinecraftServer> consumer) {
        SERVER_STARTING_EVENT.add(consumer);
    }

    public static List<Consumer<MinecraftServer>> SERVER_STOPPING_EVENT = new ObjectArrayList<>();
    public static void registerServerStoppingEvent(Consumer<MinecraftServer> consumer) {
        SERVER_STOPPING_EVENT.add(consumer);
    }

    public static List<Consumer<MinecraftServer>> TICK_EVENT = new ObjectArrayList<>();
    public static void registerTickEvent(Consumer<MinecraftServer> consumer) {
        TICK_EVENT.add(consumer);
    }

    @EventBusSubscriber
    public static class ServerEventBusListener {

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            for (Consumer<ServerPlayer> consumer : PLAYER_JOIN_EVENT) {
                consumer.accept((ServerPlayer) event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            for (Consumer<ServerPlayer> consumer : PLAYER_JOIN_EVENT) {
                consumer.accept((ServerPlayer) event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            for (Consumer<ServerPlayer> consumer : PLAYER_QUIT_EVENT) {
                consumer.accept((ServerPlayer) event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            for (Consumer<MinecraftServer> consumer : SERVER_STARTING_EVENT) {
                consumer.accept(event.getServer());
            }
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            for (Consumer<MinecraftServer> consumer : SERVER_STOPPING_EVENT) {
                consumer.accept(event.getServer());
            }
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Pre event) {
            for (Consumer<MinecraftServer> consumer : TICK_EVENT) {
                consumer.accept(event.getServer());
            }
        }
    }

    public static void sendPacketToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
        packet.readerIndex(0);
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.sendS2C(player, id, packet);
#else
        NetworkManager.sendToPlayer(player, id, packet);
#endif
    }
}