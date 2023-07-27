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

public class PacketEntryActionC2S {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "entry_action");

    public static final int ACTION_DELETE = -1;
    public static final int ACTION_LIKE = 1;

    public static class ClientLogics {

        public static void send(CommentEntry comment, int action) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeResourceLocation(comment.level);
            switch (action) {
                case ACTION_DELETE -> {
                    comment.deleted = true;
                }
                case ACTION_LIKE -> {
                    comment.like++;
                }
            }
            comment.writeBuffer(buffer, false);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        ResourceLocation level = buffer.readResourceLocation();
        CommentEntry comment = new CommentEntry(level, buffer, false);
        if (!initiator.hasPermissions(3)
                && !comment.initiator.equals(initiator.getGameProfile().getId())) {
            return;
        }
        try {
            Main.DATABASE.comments.update(comment);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PacketEntryUpdateS2C.send(player, comment, true);
            }
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
