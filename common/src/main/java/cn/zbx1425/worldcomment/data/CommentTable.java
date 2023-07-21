package cn.zbx1425.worldcomment.data;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

public class CommentTable {

    public final Database db;

    Map<ResourceLocation, Long2ObjectMap<List<CommentEntry>>> regionIndex = new HashMap<>();
    Map<UUID, List<CommentEntry>> playerIndex = new HashMap<>();

    public CommentTable(Database db) {
        this.db = db;
    }

    public void load() throws IOException {
        Files.createDirectories(db.basePath.resolve("regions"));
        for (Level level : db.server.getAllLevels()) {
            ResourceLocation dimension = level.dimension().location();
            Path levelPath = getLevelPath(dimension);
            Files.createDirectory(levelPath);
            regionIndex.put(dimension, new Long2ObjectOpenHashMap<>());
            try (Stream<Path> files = Files.list(levelPath)) {
                for (Path file : files.toList()) {
                    long region = Long.parseUnsignedLong(file.getFileName().toString().substring(1, 9), 16);
                    List<CommentEntry> regionEntries = new ArrayList<>();
                    regionIndex.get(dimension).put(region, regionEntries);

                    byte[] fileContent = Files.readAllBytes(file);
                    FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(fileContent));
                    while (src.readerIndex() < fileContent.length - 1) {
                        CommentEntry entry = new CommentEntry(dimension, src);
                        regionEntries.add(entry);
                        playerIndex.computeIfAbsent(entry.initiator, ignored -> new ArrayList<>())
                                .add(entry);
                    }
                }
            }
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

    public List<CommentEntry> queryInRegion(ResourceLocation level, ChunkPos region) throws SQLException {
        return regionIndex.get(level).get(region.toLong());
    }

    public void insert(CommentEntry newEntry) throws IOException {
        Path targetFile = getLevelRegionPath(newEntry.level, newEntry.region);
        try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), true)) {
            newEntry.writeFileStream(oStream);
        }
        regionIndex.get(newEntry.level)
                .computeIfAbsent(newEntry.region.toLong(), ignored -> new ArrayList<>())
                .add(newEntry);
        playerIndex.computeIfAbsent(newEntry.initiator, ignored -> new ArrayList<>())
                .add(newEntry);
    }
}
