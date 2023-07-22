package cn.zbx1425.worldcomment.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class CommentTable {

    public final Database db;

    Map<ResourceLocation, Long2ObjectMap<List<CommentEntry>>> regionIndex = new HashMap<>();
    Map<UUID, List<CommentEntry>> playerIndex = new HashMap<>();
    Long2ObjectSortedMap<CommentEntry> timeIndex = new Long2ObjectAVLTreeMap<>(Comparator.reverseOrder());

    public CommentTable(Database db) {
        this.db = db;
    }

    public void load() throws IOException {
        if (db.isHost) {
            Files.createDirectories(db.basePath.resolve("regions"));
            for (Level level : db.server.getAllLevels()) {
                ResourceLocation dimension = level.dimension().location();
                Path levelPath = getLevelPath(dimension);
                Files.createDirectory(levelPath);
                try (Stream<Path> files = Files.list(levelPath)) {
                    for (Path file : files.toList()) {
                        long region = Long.parseUnsignedLong(file.getFileName().toString().substring(1, 9), 16);
                        byte[] fileContent = Files.readAllBytes(file);
                        loadRegion(dimension, region, fileContent);
                    }
                }
            }
        } else {

        }
    }

    public void loadRegion(ResourceLocation dimension, long region, byte[] data) {
        synchronized (this) {
            List<CommentEntry> regionEntries = new ArrayList<>();
            FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            while (src.readerIndex() < data.length) {
                CommentEntry entry = new CommentEntry(dimension, src);
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
        return db.basePath.resolve("regions")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath());
    }

    private Path getLevelRegionPath(ResourceLocation dimension, ChunkPos region) {
        return db.basePath.resolve("regions")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath())
                .resolve("r" + Long.toHexString(region.toLong()) + ".bin");
    }

    public List<CommentEntry> queryRegion(ResourceLocation level, ChunkPos region) {
        synchronized (this) {
            return regionIndex.get(level).get(region.toLong());
        }
    }

    public List<CommentEntry> queryPlayer(UUID player) {
        synchronized (this) {
            return playerIndex.get(player);
        }
    }

    public List<CommentEntry> queryLatest(int offset, int count) {
        synchronized (this) {
            return timeIndex.values().stream().skip(offset).limit(count).toList();
        }
    }

    public void insert(CommentEntry newEntry) throws IOException {
        synchronized (this) {
            if (db.isHost) {
                Path targetFile = getLevelRegionPath(newEntry.level, newEntry.region);
                try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), true)) {
                    newEntry.writeFileStream(oStream);
                }
            }
            regionIndex.get(newEntry.level)
                    .computeIfAbsent(newEntry.region.toLong(), ignored -> new ArrayList<>())
                    .add(newEntry);
            playerIndex.computeIfAbsent(newEntry.initiator, ignored -> new ArrayList<>())
                    .add(newEntry);
        }
    }
}
