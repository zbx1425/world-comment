package cn.zbx1425.worldcomment.data.persist;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldMeta;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileSerializer {

    private final Path basePath;

    public FileSerializer(Path basePath) {
        this.basePath = basePath;
    }

    public ServerWorldMeta loadInto(CommentCache commentCache) throws IOException {
        commentCache.clear();
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
                        if (fileNameParts.length != 4 || !fileNameParts[3].equals("bin")) continue;
                        ChunkPos region = new ChunkPos(Integer.parseInt(fileNameParts[1]), Integer.parseInt(fileNameParts[2]));
                        byte[] fileContent = Files.readAllBytes(file);
                        commentCache.loadRegion(dimension, region.pack(), fileContent, true);
                    }
                }
            }
        }

        if (Files.exists(basePath.resolve("metadata.json"))) {
            String metaContent = Files.readString(basePath.resolve("metadata.json"));
            return new ServerWorldMeta(JsonParser.parseString(metaContent).getAsJsonObject());
        } else {
            ServerWorldMeta meta = new ServerWorldMeta();
            Files.writeString(basePath.resolve("metadata.json"), meta.serialize().toString());
            return meta;
        }
    }

    private Path getLevelPath(Identifier dimension) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath());
    }

    private Path getLevelRegionPath(Identifier dimension, ChunkPos region) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath())
                .resolve("r." + region.x() + "." + region.z() + ".bin");
    }

    public void insert(CommentEntry newEntry) throws IOException {
        synchronized (this) {
            try {
                Files.createDirectory(getLevelPath(newEntry.level));
            } catch (FileAlreadyExistsException ignored) {
            }
            Path targetFile = getLevelRegionPath(newEntry.level, newEntry.region);
            try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), true)) {
                newEntry.writeFileStream(oStream);
            }
        }
    }

    public void update(CommentEntry existingEntry) throws IOException {
        synchronized (this) {
            assert existingEntry.fileOffset > 0;
            Path targetFile = getLevelRegionPath(existingEntry.level, existingEntry.region);
            try (RandomAccessFile oStream = new RandomAccessFile(targetFile.toFile(), "rw")) {
                existingEntry.updateInFile(oStream);
            }
        }
    }

    public void updateRegion(List<CommentEntry> regionEntries) throws IOException {
        synchronized (this) {
            if (regionEntries.isEmpty()) return;
            CommentEntry pivot = regionEntries.get(0);
            try {
                Files.createDirectory(getLevelPath(pivot.level));
            } catch (FileAlreadyExistsException ignored) {
            }
            Path targetFile = getLevelRegionPath(pivot.level, pivot.region);
            try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), false)) {
                for (CommentEntry entry : regionEntries) {
                    entry.writeFileStream(oStream);
                }
            }
        }
    }
}
