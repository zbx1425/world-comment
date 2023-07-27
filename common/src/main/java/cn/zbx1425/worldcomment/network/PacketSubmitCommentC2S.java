package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public class PacketSubmitCommentC2S {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "submit_comment");

    public static class ClientLogics {

        public static void send(CommentEntry comment) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeResourceLocation(comment.level);
            comment.writeBuffer(buffer, false);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        ResourceLocation level = buffer.readResourceLocation();
        CommentEntry comment = new CommentEntry(level, buffer, false);
        if (!comment.initiator.equals(Util.NIL_UUID)
                && !comment.initiator.equals(initiator.getGameProfile().getId())) {
            return;
        }
        try {
            Main.DATABASE.comments.insert(comment);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                /*
                int playerCX = player.blockPosition().getX() >> 4;
                int playerCZ = player.blockPosition().getX() >> 4;
                if (Math.abs(comment.region.x - playerCX) <= 1 && Math.abs(comment.region.z - playerCZ) <= 1) {

                }
                 */
                PacketCommentUpdateS2C.send(player, comment, false);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
