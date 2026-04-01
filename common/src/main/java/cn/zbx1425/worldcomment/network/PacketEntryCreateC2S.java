package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCommand;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;

public class PacketEntryCreateC2S {

    public static final Identifier IDENTIFIER = Main.id("entry_create");

    public static class ClientLogics {

        public static void send(CommentEntry comment) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeIdentifier(comment.level);
            comment.writeBuffer(buffer, false);
            if (CommentCommand.isCommand(comment)) {
                CommentCommand.executeCommandClient(comment);
            }
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        Identifier level = buffer.readIdentifier();
        CommentEntry comment = new CommentEntry(level, buffer, false);
        if (!comment.initiator.equals(initiator.getGameProfile().id())) return;
        if (comment.message.length() > CommentEntry.MESSAGE_MAX_LENGTH) return;
        if (CommentCommand.isCommand(comment) && !initiator.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) return;
        try {
            Main.DATABASE.insert(comment, false);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
