package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketCollectionRequestC2S {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "collection_request");

    public static class ClientLogics {

        public static void sendPlayer(UUID playerId, long nonce) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeInt(1);
            buffer.writeLong(nonce);
            buffer.writeUUID(playerId);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }

        public static void sendLatest(int offset, int limit, long nonce) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeInt(2);
            buffer.writeLong(nonce);
            buffer.writeInt(offset);
            buffer.writeInt(limit);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        int requestType = buffer.readInt();
        long nonce = buffer.readLong();

        switch (requestType) {
            case 1 -> {
                UUID playerId = buffer.readUUID();
                PacketCollectionDataS2C.send(initiator, Main.DATABASE.comments.queryPlayer(playerId), nonce);
            }
            case 2 -> {
                int offset = buffer.readInt();
                int limit = buffer.readInt();
                PacketCollectionDataS2C.send(initiator, Main.DATABASE.comments.queryLatest(offset, limit), nonce);
            }
        }
    }
}
