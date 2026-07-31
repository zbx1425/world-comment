package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommentChunk {

    private final List<CommentEntry> entries = new ArrayList<>();
    private boolean dirty = false;

    public List<CommentEntry> getEntries() {
        return entries;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void add(CommentEntry entry) {
        entries.add(entry);
        dirty = true;
    }

    public static CommentChunk load(Path file) throws IOException {
        CommentChunk chunk = new CommentChunk();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    CommentEntry entry = CommentEntry.fromJson(json);
                    chunk.entries.add(entry);
                } catch (Exception e) {
                    Main.LOGGER.warn("Skipping malformed JSONL line in {}: {}", file, e.getMessage());
                }
            }
        }
        chunk.dirty = false;
        return chunk;
    }

    public void saveTo(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (CommentEntry entry : entries) {
                writer.write(entry.toJson().toString());
                writer.newLine();
            }
        }
        dirty = false;
    }

    public List<CommentEntry> getActiveEntries() {
        List<CommentEntry> result = new ArrayList<>();
        for (CommentEntry entry : entries) {
            if (!entry.deleted) result.add(entry);
        }
        return result;
    }
}
