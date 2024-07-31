package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.Map;

public class PacketRegionRequestC2S {

    public static final ResourceLocation IDENTIFIER = Main.id("request_region");

    public static class ClientLogics {

        public static void send(ResourceLocation level, List<ChunkPos> requests) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeResourceLocation(level);
            buffer.writeInt(requests.size());
            for (ChunkPos request : requests) {
                buffer.writeChunkPos(request);
            }
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        ResourceLocation level = buffer.readResourceLocation();
        Map<ChunkPos, List<CommentEntry>> results = new Object2ObjectArrayMap<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++) {
            ChunkPos request = buffer.readChunkPos();
            List<CommentEntry> regionResults = Main.DATABASE.comments.queryRegion(level, request);
            results.put(request, regionResults);
        }
        PacketRegionDataS2C.send(initiator, level, results);
    }
}
