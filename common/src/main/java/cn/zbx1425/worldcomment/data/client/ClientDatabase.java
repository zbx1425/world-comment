package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.network.PacketRequestRegionC2S;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class ClientDatabase {

    public ResourceLocation level;
    public Long2ObjectMap<List<CommentEntry>> regions = new Long2ObjectOpenHashMap<>();
    public Long2LongMap regionExpiry = new Long2LongOpenHashMap();

    public static ClientDatabase INSTANCE = new ClientDatabase();

    public static final long REGION_TTL = 60000;

    private long lastTickTime = 0;

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTickTime < 1000) return;
        lastTickTime = currentTime;

        BlockPos playerPos = minecraft.player.blockPosition();
        int cx = playerPos.getX() >> (4 + CommentEntry.REGION_SHIFT);
        int cz = playerPos.getZ() >> (4 + CommentEntry.REGION_SHIFT);

        List<ChunkPos> regionsToRequest = new ArrayList<>();
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                long chunkLong = ChunkPos.asLong(x, z);
                if (!regionExpiry.containsKey(chunkLong) || (regionExpiry.get(chunkLong) > currentTime)) {
                    regionExpiry.put(chunkLong, currentTime + REGION_TTL);
                    regionsToRequest.add(new ChunkPos(x, z));
                }
            }
        }
        PacketRequestRegionC2S.ClientLogics.send(minecraft.level.dimension().location(), regionsToRequest);

        for (ObjectIterator<Long2LongMap.Entry> it = regionExpiry.long2LongEntrySet().iterator(); it.hasNext(); ) {
            Long2LongMap.Entry entry = it.next();
            if (entry.getLongValue() > currentTime) {
                regions.remove(entry.getLongKey());
                it.remove();
            }
        }
    }

    public void clear() {
        regions.clear();
        regionExpiry.clear();
    }

    public void acceptRegions(ResourceLocation level, Long2ObjectMap<List<CommentEntry>> regions) {
        if (!level.equals(this.level)) clear();
        this.level = level;
        long currentTime = System.currentTimeMillis();
        for (Long2ObjectMap.Entry<List<CommentEntry>> entry : regions.long2ObjectEntrySet()) {
            regions.put(entry.getLongKey(), entry.getValue());
            regionExpiry.put(entry.getLongKey(), currentTime + REGION_TTL);
        }
    }
}
