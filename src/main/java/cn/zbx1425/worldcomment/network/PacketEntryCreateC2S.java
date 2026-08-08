package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.platform.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCommand;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class PacketEntryCreateC2S {

    public static final Identifier IDENTIFIER = Main.id("entry_create");

    public static class ClientLogics {

        public static void send(CommentEntry comment) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeIdentifier(comment.level);
            comment.writeBuffer(buffer);
            if (CommentCommand.isCommand(comment)) {
                CommentCommand.executeCommandClient(comment);
            }
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        Identifier level = buffer.readIdentifier();
        CommentEntry comment = new CommentEntry(level, buffer);
        if (!comment.initiator.equals(initiator.getGameProfile().id())) return;
        if (comment.message.length() > CommentEntry.MESSAGE_MAX_LENGTH) return;
        if (CommentCommand.isCommand(comment) && !initiator.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) return;
        if (CommentEntry.isMarkerType(comment.messageType)) {
            boolean allowed = switch (Main.SERVER_CONFIG.allowMarkerUsage.value) {
                case OP -> initiator.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                case CREATIVE -> initiator.isCreative()
                    || initiator.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
                case ALL -> true;
            };
            if (!allowed) return;
        }
        Main.DATABASE.insert(comment, false);
    }
}
