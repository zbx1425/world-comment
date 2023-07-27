package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientDatabase;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketEntryUpdateS2C {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "entry_update");

    public static void send(ServerPlayer target, CommentEntry comment, boolean update) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBoolean(update);
        buffer.writeResourceLocation(comment.level);
        comment.writeBuffer(buffer, false);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            boolean update = buffer.readBoolean();
            ResourceLocation level = buffer.readResourceLocation();
            CommentEntry comment = new CommentEntry(level, buffer, false);
            ClientDatabase.INSTANCE.acceptUpdate(comment, update);
        }
    }
}
