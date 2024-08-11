package cn.zbx1425.worldcomment.interop;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProximityCommentSet {

    public final double distanceSqr;
    public final long timeToLive;

    public final Object2LongMap<CommentEntry> comments = new Object2LongOpenHashMap<>();

    public Consumer<CommentEntry> onCommentApproach;

    public ProximityCommentSet(double distance, long timeToLive) {
        this.distanceSqr = distance * distance;
        this.timeToLive = timeToLive;
    }

    public void tick(ClientWorldData worldData) {
        Minecraft minecraft = Minecraft.getInstance();
        long currentTime = System.currentTimeMillis();
        BlockPos playerPos = minecraft.player.blockPosition();

        for (Map<BlockPos, List<CommentEntry>> region : ClientWorldData.INSTANCE.regions.values()) {
            for (Map.Entry<BlockPos, List<CommentEntry>> blockData : region.entrySet()) {
                for (CommentEntry comment : blockData.getValue()) {
                    if (comment.deleted) continue;
                    if (comment.location.distSqr(playerPos) <= distanceSqr) {
                        if (!comments.containsKey(comment)) {
                            if (onCommentApproach != null) {
                                onCommentApproach.accept(comment);
                            }
                        }
                        comments.put(comment, currentTime + timeToLive);
                    }
                }
            }
        }

        comments.keySet().removeIf(comment -> comments.getLong(comment) < currentTime);
    }

    public void clear() {
        comments.clear();
    }
}
