package cn.zbx1425.worldcomment.platform;

import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

//? if neoforge
import cn.zbx1425.worldcomment.platform.neoforge.ClientPlatformImpl;
//? if fabric
//import cn.zbx1425.worldcomment.platform.fabric.ClientPlatformImpl;

public class ClientPlatform {

    public static void registerKeyBinding(RegistryObject<KeyMapping> keyMapping) {
        ClientPlatformImpl.registerKeyBinding(keyMapping);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        ClientPlatformImpl.registerNetworkReceiver(resourceLocation, consumer);
    }

    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        ClientPlatformImpl.registerPlayerJoinEvent(consumer);
    }

    public static void registerPlayerLeaveEvent(Runnable runnable) {
        ClientPlatformImpl.registerPlayerLeaveEvent(runnable);
    }

    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        ClientPlatformImpl.registerTickEvent(consumer);
    }

    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
        ClientPlatformImpl.sendPacketToServer(id, packet);
    }
}
