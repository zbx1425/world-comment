package cn.zbx1425.worldcomment.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.*;
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
        regionIndex.clear();
        playerIndex.clear();
        timeIndex.clear();
        if (db.isHost) {
            Files.createDirectories(db.basePath.resolve("region"));
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
        } else {

        }
    }

    public void loadRegion(ResourceLocation dimension, long region, byte[] data, boolean fromFile) {
        synchronized (this) {
            List<CommentEntry> regionEntries = new ArrayList<>();
            FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            while (src.readerIndex() < data.length) {
                CommentEntry entry = new CommentEntry(dimension, src, fromFile);
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
            return timeIndex.values().stream().skip(offset).limit(count).toList();
        }
    }

    public void insert(CommentEntry newEntry) throws IOException {
        synchronized (this) {
            boolean createLevelFolder = !regionIndex.containsKey(newEntry.level);
            if (createLevelFolder) regionIndex.put(newEntry.level, new Long2ObjectOpenHashMap<>());
            if (db.isHost) {
                if (createLevelFolder) {
                    Files.createDirectory(getLevelPath(newEntry.level));
                }
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
