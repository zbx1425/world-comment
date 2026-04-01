package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PacketEntryUpdateS2C {

    public static final Identifier IDENTIFIER = Main.id("entry_update");

    public static void send(ServerPlayer target, CommentEntry comment, boolean update) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBoolean(update);
        buffer.writeIdentifier(comment.level);
        comment.writeBuffer(buffer, false);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            boolean update = buffer.readBoolean();
            Identifier level = buffer.readIdentifier();
            CommentEntry comment = new CommentEntry(level, buffer, false);
            ClientWorldData.INSTANCE.acceptUpdate(comment, update);
        }
    }
}
