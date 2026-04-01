package cn.zbx1425.worldcomment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class ClientPlatform {

    @ExpectPlatform
    public static void registerKeyBinding(KeyMapping keyMapping) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerPlayerJoinEvent(Consumer<LocalPlayer> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerPlayerLeaveEvent(Runnable runnable) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerTickEvent(Consumer<Minecraft> consumer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
        throw new AssertionError();
    }
}
