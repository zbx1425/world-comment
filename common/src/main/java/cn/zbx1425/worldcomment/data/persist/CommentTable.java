package cn.zbx1425.worldcomment.data.persist;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.synchronizer.*;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class CommentTable {

    public final Database db;

    Map<ResourceLocation, Long2ObjectMap<List<CommentEntry>>> regionIndex = new HashMap<>();
    Map<UUID, List<CommentEntry>> playerIndex = new HashMap<>();
    Long2ObjectSortedMap<CommentEntry> timeIndex = new Long2ObjectAVLTreeMap<>(Comparator.reverseOrder());

    private final Synchronizer synchronizer;

    public CommentTable(Database db, Synchronizer sync) {
        this.db = db;
        this.synchronizer = sync;

        try {
            this.synchronizer.sync(db.basePath.resolve("region"));
        } catch (IOException e) {
            //Todo: seems need to do something here
        }

    }

    public void load() throws IOException {
        regionIndex.clear();
        playerIndex.clear();
        timeIndex.clear();
            try {
                Files.createDirectories(db.basePath.resolve("region"));
            } catch (FileAlreadyExistsException ignored) {

            }
            try (Stream<Path> levelFiles = Files.list(db.basePath.resolve("region"))) {
                for (Path levelPath : levelFiles.toList()) {
                    ResourceLocation dimension = new ResourceLocation(levelPath.getFileName().toString().replace("+", ":"));
                    try (Stream<Path> files = Files.list(levelPath)) {
                        for (Path file : files.toList()) {
                            String[] fileNameParts = file.getFileName().toString().split("\\.");
                            if (fileNameParts.length != 4 || !fileNameParts[3].equals("bin")) continue;
                            ChunkPos region = new ChunkPos(Integer.parseInt(fileNameParts[1]), Integer.parseInt(fileNameParts[2]));
                            byte[] fileContent = Files.readAllBytes(file);
                            loadRegion(dimension, region.toLong(), fileContent, true);
                        }
                    }
                }
            }
    }

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

    private Path getLevelPath(ResourceLocation dimension) {
        return db.basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath());
    }

    private Path getLevelRegionPath(ResourceLocation dimension, ChunkPos region) {
        return db.basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath())
                .resolve("r." + region.x + "." + region.z + ".bin");
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

    public void insert(CommentEntry newEntry) throws IOException {
        synchronized (this) {
            boolean createLevelFolder = !regionIndex.containsKey(newEntry.level);
            if (createLevelFolder) regionIndex.put(newEntry.level, new Long2ObjectOpenHashMap<>());
            if (db.isHost) {
                if (createLevelFolder) {
                    try {
                        Files.createDirectory(getLevelPath(newEntry.level));
                    } catch (FileAlreadyExistsException ignored) {

                    }
                }

                this.synchronizer.update(newEntry, getLevelRegionPath(newEntry.level, newEntry.region));
            }
            regionIndex.get(newEntry.level)
                    .computeIfAbsent(newEntry.region.toLong(), ignored -> new ArrayList<>())
                    .add(newEntry);
            playerIndex.computeIfAbsent(newEntry.initiator, ignored -> new ArrayList<>())
                    .add(newEntry);
            timeIndex.put(newEntry.timestamp, newEntry);
        }
    }

    public void update(CommentEntry newEntry) throws IOException {
        synchronized (this) {
            List<CommentEntry> regionData = regionIndex.getOrDefault(newEntry.level, Long2ObjectMaps.emptyMap())
                    .get(newEntry.region.toLong());
            if (regionData == null) return;
            for (CommentEntry existingEntry : regionData) {
                if (existingEntry.id == newEntry.id) {
                    //Todo: refactor this
                    existingEntry.deleted = newEntry.deleted;
                    existingEntry.like = newEntry.like;
                    assert existingEntry.fileOffset > 0;
                    if (db.isHost) {
                        this.synchronizer.update(existingEntry, getLevelRegionPath(newEntry.level, newEntry.region));
                    }
                }
            }
        }
    }
}
