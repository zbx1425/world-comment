package cn.zbx1425.worldcomment.data.persist;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileSerializer {

    private final Path basePath;

    public FileSerializer(Path basePath) {
        this.basePath = basePath;
    }

    public void loadInto(CommentCache commentCache) throws IOException {
        commentCache.clear();
        try {
            Files.createDirectories(basePath.resolve("region"));
        } catch (FileAlreadyExistsException ignored) { }
        try (Stream<Path> levelFiles = Files.list(basePath.resolve("region"))) {
            for (Path levelPath : levelFiles.toList()) {
#if MC_VERSION >= "12100"
                ResourceLocation dimension = ResourceLocation.parse(levelPath.getFileName().toString().replace("+", ":"));
#else
                ResourceLocation dimension = new ResourceLocation(levelPath.getFileName().toString().replace("+", ":"));
#endif
                try (Stream<Path> files = Files.list(levelPath)) {
                    for (Path file : files.toList()) {
                        String[] fileNameParts = file.getFileName().toString().split("\\.");
                        if (fileNameParts.length != 4 || !fileNameParts[3].equals("bin")) continue;
                        ChunkPos region = new ChunkPos(Integer.parseInt(fileNameParts[1]), Integer.parseInt(fileNameParts[2]));
                        byte[] fileContent = Files.readAllBytes(file);
                        commentCache.loadRegion(dimension, region.toLong(), fileContent, true);
                    }
                }
            }
        }
    }

    private Path getLevelPath(ResourceLocation dimension) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath());
    }

    private Path getLevelRegionPath(ResourceLocation dimension, ChunkPos region) {
        return basePath.resolve("region")
                .resolve(dimension.getNamespace() + "+" + dimension.getPath())
                .resolve("r." + region.x + "." + region.z + ".bin");
    }

    public void insert(CommentEntry newEntry) throws IOException {
        try {
            Files.createDirectory(getLevelPath(newEntry.level));
        } catch (FileAlreadyExistsException ignored) { }
        Path targetFile = getLevelRegionPath(newEntry.level, newEntry.region);
        try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), true)) {
            newEntry.writeFileStream(oStream);
        }
    }

    public void cover(CommentEntry newEntry, boolean append) throws IOException {
        try {
            Files.createDirectory(getLevelPath(newEntry.level));
        } catch (FileAlreadyExistsException ignored) { }

        Path targetFile = getLevelRegionPath(newEntry.level, newEntry.region);
        try (FileOutputStream oStream = new FileOutputStream(targetFile.toFile(), append)) {
            newEntry.writeFileStream(oStream);
        }
    }

    public void update(CommentEntry existingEntry) throws IOException {
        assert existingEntry.fileOffset > 0;
        Path targetFile = getLevelRegionPath(existingEntry.level, existingEntry.region);
        try (RandomAccessFile oStream = new RandomAccessFile(targetFile.toFile(), "rw")) {
            existingEntry.updateInFile(oStream);
        }
    }
}
