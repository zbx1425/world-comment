package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageDump;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PacketCollectionDataS2C {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "collection_data");

    public static void send(ServerPlayer target, List<CommentEntry> data, long nonce) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(nonce);
        buffer.writeInt(data.size());
        for (CommentEntry comment : data) {
            buffer.writeResourceLocation(comment.level);
            comment.writeBuffer(buffer, false);
        }
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            long nonce = buffer.readLong();
            int commentSize = buffer.readInt();
            ArrayList<CommentEntry> comments = new ArrayList<>(commentSize);
            for (int j = 0; j < commentSize; j++) {
                ResourceLocation level = buffer.readResourceLocation();
                CommentEntry comment = new CommentEntry(level, buffer, false);
                if (comment.deleted) continue;
                comments.add(comment);
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof CommentListScreen) {
                ((CommentListScreen)minecraft.screen).handleCommentDataUI(comments, nonce);
            }

            ImageDump.handleDumpResponse(comments, nonce);
        }
    }
}
