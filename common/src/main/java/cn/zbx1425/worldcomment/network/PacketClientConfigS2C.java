package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketClientConfigS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("client_config");

    public static void send(ServerPlayer target, ServerConfig config) {
        ClientConfig clientCfg = ClientConfig.fromServerConfig(config);
        send(target, clientCfg);
    }

    public static void send(ServerPlayer target, ClientConfig config) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        config.writePacket(buffer);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            MainClient.CLIENT_CONFIG.readPacket(buffer);
        }
    }
}
