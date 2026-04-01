package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;

public class PacketEntryActionC2S {

    public static final Identifier IDENTIFIER = Main.id("entry_action");

    public static final int ACTION_DELETE = -1;
    public static final int ACTION_LIKE = 1;

    public static class ClientLogics {

        public static void send(CommentEntry comment, int action) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeIdentifier(comment.level);
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
        Identifier level = buffer.readIdentifier();
        CommentEntry comment = new CommentEntry(level, buffer, false);
        if (!initiator.permissions().hasPermission(Permissions.COMMANDS_ADMIN)
                && !comment.initiator.equals(initiator.getGameProfile().id())) {
            return;
        }
        try {
            Main.DATABASE.update(comment, false);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
