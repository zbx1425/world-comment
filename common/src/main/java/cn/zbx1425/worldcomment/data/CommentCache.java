package cn.zbx1425.worldcomment.data;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.util.*;

public class CommentCache {

    Map<ResourceLocation, Long2ObjectMap<List<CommentEntry>>> regionIndex = new HashMap<>();
    Map<UUID, List<CommentEntry>> playerIndex = new HashMap<>();
    Long2ObjectSortedMap<CommentEntry> timeIndex = new Long2ObjectAVLTreeMap<>(Comparator.reverseOrder());

    public void loadRegion(ResourceLocation dimension, long region, byte[] data, boolean fromFile) {
        synchronized (this) {
            List<CommentEntry> regionEntries = new ArrayList<>();
            FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            while (src.readerIndex() < data.length) {
                CommentEntry entry = new CommentEntry(dimension, src, fromFile);
                if (entry.deleted) continue;
                regionEntries.add(entry);
                playerIndex.computeIfAbsent(entry.initiator, ignored -> new ArrayList<>())
                        .add(entry);
                timeIndex.put(entry.timestamp, entry);
            }
            regionIndex.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>())
                    .put(region, regionEntries);
        }
    }

    public List<CommentEntry> queryRegion(ResourceLocation level, ChunkPos region) {
        synchronized (this) {
            return regionIndex
                    .getOrDefault(level, Long2ObjectMaps.emptyMap())
                    .getOrDefault(region.toLong(), List.of());
        }
    }

    public List<CommentEntry> queryPlayer(UUID player) {
        synchronized (this) {
            return playerIndex
                    .getOrDefault(player, List.of());
        }
    }

    public List<CommentEntry> queryLatest(int offset, int count) {
        synchronized (this) {
            List<CommentEntry> result = new ArrayList<>();
            for (CommentEntry comment : timeIndex.values()) {
                if (comment.deleted) continue;
                if (offset > 0) {
                    offset--;
                    continue;
                }
                if (count <= 0) break;
                result.add(comment);
                count--;
            }
            return result;
        }
    }

    public void insert(CommentEntry newEntry) {
        synchronized (this) {
            regionIndex.computeIfAbsent(newEntry.level, ignored -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(newEntry.region.toLong(), ignored -> new ArrayList<>())
                    .add(newEntry);
            playerIndex.computeIfAbsent(newEntry.initiator, ignored -> new ArrayList<>())
                    .add(newEntry);
            timeIndex.put(newEntry.timestamp, newEntry);
        }
    }

    // Update only the patch-able fields
    public CommentEntry update(CommentEntry newEntry) {
        synchronized (this) {
            List<CommentEntry> regionData = regionIndex.getOrDefault(newEntry.level, Long2ObjectMaps.emptyMap())
                    .get(newEntry.region.toLong());
            if (regionData == null) return null;
            for (CommentEntry existingEntry : regionData) {
                if (existingEntry.id == newEntry.id) {
                    existingEntry.deleted = newEntry.deleted;
                    existingEntry.uplinkSent = newEntry.uplinkSent;
                    existingEntry.like = newEntry.like;
                    assert existingEntry.fileOffset > 0;
                    return existingEntry;
                }
            }
            return null;
        }
    }

    public List<CommentEntry> updateAllFields(CommentEntry newEntry) {
        synchronized (this) {
            List<CommentEntry> regionData = regionIndex.getOrDefault(newEntry.level, Long2ObjectMaps.emptyMap())
                    .get(newEntry.region.toLong());
            if (regionData == null) return null;
            for (CommentEntry existingEntry : regionData) {
                if (existingEntry.id == newEntry.id) {
                    existingEntry.copyFrom(newEntry);
                    return regionData;
                }
            }
            return null;
        }
    }

    public void clear() {
        synchronized (this) {
            regionIndex.clear();
            playerIndex.clear();
            timeIndex.clear();
        }
    }
}