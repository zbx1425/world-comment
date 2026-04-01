package cn.zbx1425.worldcomment.forge;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientPlatformImpl {

    public static List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    public static void registerKeyBinding(KeyMapping keyMapping) {
        KEY_MAPPINGS.add(keyMapping);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, resourceLocation, (packet, context) -> consumer.accept(packet));
    }

    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(consumer::accept);
    }

    public static void registerPlayerLeaveEvent(Runnable runnable) {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> runnable.run());
    }

    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        ClientTickEvent.CLIENT_PRE.register(consumer::accept);
    }

    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
        NetworkManager.sendToServer(id, packet);
    }
}
