package cn.zbx1425.worldcomment.neoforge;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientPlatformImpl {

    public static List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKeyBinding(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
    }

    public static void registerNetworkReceiver(ResourceLocation resourceLocation, Consumer<FriendlyByteBuf> consumer) {
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
#else
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, resourceLocation, (packet, context) -> consumer.accept(packet));
#endif
    }

    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(consumer::accept);
    }

    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        ClientTickEvent.CLIENT_PRE.register(consumer::accept);
    }

    public static void sendPacketToServer(ResourceLocation id, FriendlyByteBuf packet) {
        packet.readerIndex(0);
#if MC_VERSION >= "12100"
        MainForge.PACKET_REGISTRY.sendC2S(id, packet);
#else
        NetworkManager.sendToServer(id, packet);
#endif
    }
}
