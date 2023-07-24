package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        } catch (IOException e) {
            Main.LOGGER.error("Failed to create comment", e);
        }
    }
}
