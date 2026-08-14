package cn.zbx1425.worldcomment.data;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class CommentStore {

    Map<Identifier, Long2ObjectMap<CommentChunk>> regionIndex = new HashMap<>();
    Map<UUID, List<CommentEntry>> playerIndex = new HashMap<>();
    // Keys are snowflake IDs
    Long2ObjectSortedMap<CommentEntry> timeIndex = new Long2ObjectAVLTreeMap<>(Comparator.reverseOrder());

    public void acceptLoadedChunk(Identifier dimension, long region, CommentChunk chunk) {
        synchronized (this) {
            regionIndex.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>())
                    .put(region, chunk);
            for (CommentEntry entry : chunk.getEntries()) {
                if (entry.deleted) continue;
                playerIndex.computeIfAbsent(entry.initiator, ignored -> new ArrayList<>())
                        .add(entry);
                timeIndex.put(entry.id, entry);
            }
        }
    }

    public boolean containsId(long id) {
        synchronized (this) {
            return timeIndex.containsKey(id);
        }
    }

    public List<CommentEntry> queryRegion(Identifier level, ChunkPos region) {
        synchronized (this) {
            CommentChunk chunk = regionIndex
                    .getOrDefault(level, Long2ObjectMaps.emptyMap())
                    .get(region.pack());
            if (chunk == null) return List.of();
            return chunk.getActiveEntries();
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
            CommentChunk chunk = regionIndex
                    .computeIfAbsent(newEntry.level, ignored -> new Long2ObjectOpenHashMap<>())
                    .computeIfAbsent(newEntry.region.pack(), ignored -> new CommentChunk());
            chunk.add(newEntry);
            playerIndex.computeIfAbsent(newEntry.initiator, ignored -> new ArrayList<>())
                    .add(newEntry);
            timeIndex.put(newEntry.id, newEntry);
        }
    }

    public CommentEntry update(CommentEntry newEntry) {
        synchronized (this) {
            CommentChunk chunk = regionIndex.getOrDefault(newEntry.level, Long2ObjectMaps.emptyMap())
                    .get(newEntry.region.pack());
            if (chunk == null) return null;
            for (CommentEntry existingEntry : chunk.getEntries()) {
                if (existingEntry.id == newEntry.id) {
                    existingEntry.deleted = newEntry.deleted;
                    existingEntry.like = newEntry.like;
                    chunk.markDirty();
                    return existingEntry;
                }
            }
            return null;
        }
    }

    public void updateAllFields(CommentEntry newEntry) {
        synchronized (this) {
            CommentChunk chunk = regionIndex.getOrDefault(newEntry.level, Long2ObjectMaps.emptyMap())
                    .get(newEntry.region.pack());
            if (chunk == null) return;
            for (CommentEntry existingEntry : chunk.getEntries()) {
                if (existingEntry.id == newEntry.id) {
                    existingEntry.copyUpdateableFrom(newEntry);
                    chunk.markDirty();
                    return;
                }
            }
        }
    }

    public Map<Identifier, Long2ObjectMap<CommentChunk>> getRegions() {
        return regionIndex;
    }

    public void clear() {
        synchronized (this) {
            regionIndex.clear();
            playerIndex.clear();
            timeIndex.clear();
        }
    }
}
