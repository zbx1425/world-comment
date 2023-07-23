package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientDatabase;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PacketRegionDataS2C {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MOD_ID, "region_data");

    public static void send(ServerPlayer target, ResourceLocation level, Map<ChunkPos, List<CommentEntry>> data) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeResourceLocation(level);
        buffer.writeInt(data.size());
        for (Map.Entry<ChunkPos, List<CommentEntry>> entry : data.entrySet()) {
            buffer.writeChunkPos(entry.getKey());
            buffer.writeInt(entry.getValue().size());
            buffer.writeZero(16 - (buffer.writerIndex() % 16));
            for (CommentEntry comment : entry.getValue()) {
                comment.writeBuffer(buffer);
            }
        }
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {

        public static void handle(FriendlyByteBuf buffer) {
            ResourceLocation level = buffer.readResourceLocation();
            int regionSize = buffer.readInt();
            Long2ObjectMap<List<CommentEntry>> regions = new Long2ObjectOpenHashMap<>();
            for (int i = 0; i < regionSize; i++) {
                ChunkPos region = buffer.readChunkPos();
                int commentSize = buffer.readInt();
                buffer.skipBytes(16 - (buffer.readerIndex() % 16));
                ArrayList<CommentEntry> comments = new ArrayList<>(commentSize);
                for (int j = 0; j < commentSize; j++) {
                    CommentEntry comment = new CommentEntry(level, buffer);
                    comments.add(comment);
                }
                regions.put(region.toLong(), comments);
            }
            ClientDatabase.INSTANCE.acceptRegions(level, regions);
        }
    }
}
