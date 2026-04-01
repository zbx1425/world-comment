package cn.zbx1425.worldcomment.neoforge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientPlatformImpl {

    public static List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKeyBinding(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
#else
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, resourceLocation, (packet, context) -> consumer.accept(packet));
#endif
    }

    public static List<Consumer<LocalPlayer>> PLAYER_JOIN_EVENT = new ObjectArrayList<>();
    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        PLAYER_JOIN_EVENT.add(consumer);
    }

    public static List<Runnable> PLAYER_QUIT_EVENT = new ObjectArrayList<>();
    public static void registerPlayerLeaveEvent(Runnable runnable) {
        PLAYER_QUIT_EVENT.add(runnable);
    }

    public static List<Consumer<Minecraft>> TICK_EVENT = new ObjectArrayList<>();
    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        TICK_EVENT.add(consumer);
    }

    public static class ClientEventBusListener {


        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!event.getEntity().level().isClientSide()) return;
            for (Consumer<LocalPlayer> consumer : PLAYER_JOIN_EVENT) {
                consumer.accept((LocalPlayer) event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (!event.getEntity().level().isClientSide()) return;
            for (Consumer<LocalPlayer> consumer : PLAYER_JOIN_EVENT) {
                consumer.accept((LocalPlayer) event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            for (Runnable consumer : PLAYER_QUIT_EVENT) {
                consumer.run();
            }
        }
    }

    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
        packet.readerIndex(0);
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.sendC2S(id, packet);
#else
        NetworkManager.sendToServer(id, packet);
#endif
    }
}
