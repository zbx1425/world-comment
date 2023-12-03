package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCommand;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

public class PacketEntryCreateC2S {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "entry_create");

    public static class ClientLogics {

        public static void send(CommentEntry comment) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeResourceLocation(comment.level);
            comment.writeBuffer(buffer, false);
            if (CommentCommand.isCommand(comment)) {
                CommentCommand.executeCommandClient(comment);
            }
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        ResourceLocation level = buffer.readResourceLocation();
        CommentEntry comment = new CommentEntry(level, buffer, false);
        if (!comment.initiator.equals(initiator.getGameProfile().getId())) return;
        if (comment.message.length() > CommentEntry.MESSAGE_MAX_LENGTH) return;
        if (CommentCommand.isCommand(comment) && !initiator.hasPermissions(3)) return;
        try {
            Main.DATABASE.insert(comment, false);
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
