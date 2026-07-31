package cn.zbx1425.worldcomment.data.persist;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.*;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

public class CommentRepository {

    private final Path basePath;

    public CommentRepository(Path basePath) {
        this.basePath = basePath;
    }

    public ServerWorldMeta loadInto(CommentStore commentStore) throws IOException {
        commentStore.clear();

        ServerWorldMeta worldMeta;
        if (Files.exists(basePath.resolve("metadata.json"))) {
            String metaContent = Files.readString(basePath.resolve("metadata.json"));
            worldMeta = new ServerWorldMeta(JsonParser.parseString(metaContent).getAsJsonObject());
        } else {
            worldMeta = new ServerWorldMeta();
            Files.writeString(basePath.resolve("metadata.json"), worldMeta.serialize().toString());
        }

        try {
            Files.createDirectories(basePath.resolve("region"));
        } catch (FileAlreadyExistsException ignored) { }
        try (Stream<Path> levelFiles = Files.list(basePath.resolve("region"))) {
            for (Path levelPath : levelFiles.toList()) {
#if MC_VERSION >= "12100"
                Identifier dimension = Identifier.parse(levelPath.getFileName().toString().replace("+", ":"));
#else
                Identifier dimension = new Identifier(levelPath.getFileName().toString().replace("+", ":"));
#endif
                try (Stream<Path> files = Files.list(levelPath)) {
                    for (Path file : files.toList()) {
                        String[] fileNameParts = file.getFileName().toString().split("\\.");
                        if (fileNameParts.length != 4 || !fileNameParts[3].equals("dat")) continue;
                        try {
                            ChunkPos region = new ChunkPos(Integer.parseInt(fileNameParts[1]), Integer.parseInt(fileNameParts[2]));
                            CommentChunk chunk = CommentChunk.load(file, worldMeta);
                            commentStore.acceptLoadedChunk(dimension, region.pack(), chunk);
                        } catch (IOException ex) {
                            Main.LOGGER.error("Failed to load comments from {}", file, ex);
                        }
                    }
                }
            }
        }

        return worldMeta;
    }

    private Path getLevelPath(Identifier dimension) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath());
    }

    private Path getChunkPath(Identifier dimension, ChunkPos region) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath())
                .resolve("r." + region.x() + "." + region.z() + ".dat");
    }

    public void saveDirtyChunks(CommentStore store, ServerWorldMeta worldMeta) {
        for (Map.Entry<Identifier, Long2ObjectMap<CommentChunk>> dimEntry : store.getRegions().entrySet()) {
            Identifier dimension = dimEntry.getKey();
            for (Long2ObjectMap.Entry<CommentChunk> regionEntry : dimEntry.getValue().long2ObjectEntrySet()) {
                CommentChunk chunk = regionEntry.getValue();
                if (!chunk.isDirty()) continue;
                ChunkPos chunkPos = ChunkPos.unpack(regionEntry.getLongKey());
                try {
                    Path levelPath = getLevelPath(dimension);
                    Files.createDirectories(levelPath);

                    Path targetFile = getChunkPath(dimension, chunkPos);
                    Path tmpFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
                    chunk.saveTo(tmpFile, worldMeta);
                    Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    Main.LOGGER.error("Failed to save chunk {}", chunkPos, e);
                }
            }
        }
    }
}
