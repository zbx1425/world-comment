package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.network.PacketRegionRequestC2S;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientDatabase {

    public ResourceLocation level;
    public Long2ObjectMap<Map<BlockPos, List<CommentEntry>>> regions = new Long2ObjectOpenHashMap<>();
    public Long2LongMap regionExpiry = new Long2LongOpenHashMap();

    public static ClientDatabase INSTANCE = new ClientDatabase();

    public static final long REGION_TTL = 300000;

    private long lastTickTime = 0;

    public void tick() {
        synchronized (this) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) return;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTickTime < 1000) return;
            lastTickTime = currentTime;

            if (minecraft.level.dimension().location() != this.level) {
                this.level = minecraft.level.dimension().location();
                clear();
            }

            BlockPos playerPos = minecraft.player.blockPosition();
            int cx = playerPos.getX() >> (4 + CommentEntry.REGION_SHIFT);
            int cz = playerPos.getZ() >> (4 + CommentEntry.REGION_SHIFT);

            List<ChunkPos> regionsToRequest = new ArrayList<>();
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    long chunkLong = ChunkPos.asLong(x, z);
                    if (!regionExpiry.containsKey(chunkLong) || (regionExpiry.get(chunkLong) < currentTime)) {
                        regionExpiry.put(chunkLong, currentTime + REGION_TTL);
                        regionsToRequest.add(new ChunkPos(x, z));
                    }
                }
            }
            if (regionsToRequest.size() > 0) {
                PacketRegionRequestC2S.ClientLogics.send(minecraft.level.dimension().location(), regionsToRequest);
            }

            for (ObjectIterator<Long2LongMap.Entry> it = regionExpiry.long2LongEntrySet().iterator(); it.hasNext(); ) {
                Long2LongMap.Entry entry = it.next();
                if (entry.getLongValue() < currentTime) {
                    regions.remove(entry.getLongKey());
                    it.remove();
                }
            }
        }
    }

    public void clear() {
        synchronized (this) {
            regions.clear();
            regionExpiry.clear();
        }
    }

    public void acceptRegions(ResourceLocation level, Long2ObjectMap<List<CommentEntry>> regions) {
        synchronized (this) {
            if (!level.equals(this.level)) return;
            long currentTime = System.currentTimeMillis();
            for (Long2ObjectMap.Entry<List<CommentEntry>> entry : regions.long2ObjectEntrySet()) {
                this.regions.computeIfAbsent(entry.getLongKey(), ignored -> new Object2ObjectArrayMap<>()).clear();
                for (CommentEntry comment : entry.getValue()) {
                    this.regions.get(entry.getLongKey())
                            .computeIfAbsent(comment.location, ignored -> new ArrayList<>())
                            .add(comment);
                }
                regionExpiry.put(entry.getLongKey(), currentTime + REGION_TTL);
            }
        }
    }

    public void acceptUpdate(CommentEntry comment, boolean update) {
        synchronized (this) {
            if (!comment.level.equals(this.level)) return;
            Map<BlockPos, List<CommentEntry>> regionData = regions.get(comment.region.toLong());
            if (regionData != null) {
                if (update) {
                    List<CommentEntry> blockData = regionData.get(comment.location);
                    for (int i = 0; i < blockData.size(); i++) {
                        if (blockData.get(i).id == comment.id) {
                            if (comment.deleted) {
                                blockData.remove(i);
                            } else {
                                blockData.set(i, comment);
                            }
                            break;
                        }
                    }
                } else {
                    regionData.computeIfAbsent(comment.location, ignored -> new ArrayList<>()).add(comment);
                }
            }
        }
    }
}
