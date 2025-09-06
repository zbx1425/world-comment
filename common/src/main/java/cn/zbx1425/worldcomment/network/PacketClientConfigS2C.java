package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.*;
import cn.zbx1425.worldcomment.data.ServerWorldMeta;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketClientConfigS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("client_config");

    public static void send(ServerPlayer target, ServerWorldMeta worldMeta, ServerConfig config) {
        ClientConfig.ServerIssuedConfig clientCfg = new ClientConfig.ServerIssuedConfig(config);
        send(target, worldMeta, clientCfg);
    }

    public static void send(ServerPlayer target, ServerWorldMeta worldMeta, ClientConfig.ServerIssuedConfig config) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUUID(worldMeta.worldId);
        config.writePacket(buffer);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            UUID worldId = buffer.readUUID();
            String serverIp = Minecraft.getInstance().getCurrentServer() == null ? "local"
                    : Minecraft.getInstance().getCurrentServer().ip;
            MainClient.CLIENT_CONFIG.serverIssuedConfig = new ClientConfig.ServerIssuedConfig(buffer);
            MainClient.CLIENT_CONFIG.load(worldId, serverIp);
        }
    }
}
