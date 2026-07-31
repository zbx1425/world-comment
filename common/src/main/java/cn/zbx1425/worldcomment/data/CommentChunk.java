package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    public List<CommentEntry> getActiveEntries() {
        List<CommentEntry> result = new ArrayList<>();
        for (CommentEntry entry : entries) {
            if (!entry.deleted) result.add(entry);
        }
        return result;
    }

    public static CommentChunk load(Path file, ServerWorldMeta worldMeta) throws IOException {
        CommentChunk chunk = new CommentChunk();

        try {
            SecretKey key = deriveKey(worldMeta);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            try (InputStream fis = Files.newInputStream(file)) {
                byte[] iv = fis.readNBytes(16);
                if (iv.length != 16) throw new EOFException("File is too short");
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                     GZIPInputStream gis = new GZIPInputStream(cis);
                     InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(isr)) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        try {
                            JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                            CommentEntry entry = new CommentEntry(json);
                            chunk.entries.add(entry);
                        } catch (Exception e) {
                            Main.LOGGER.warn("Skipping malformed JSONL line in {}: {}", file, e.getMessage());
                        }
                    }
                }
            }
            chunk.dirty = false;
            return chunk;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to parse CommentChunk", e);
        }
    }

    public void saveTo(Path file, ServerWorldMeta worldMeta) throws IOException {
        try {
            SecretKey key = deriveKey(worldMeta);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            try (OutputStream fos = Files.newOutputStream(file)) {
                fos.write(iv);
                try (CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                     GZIPOutputStream gos = new GZIPOutputStream(cos);
                     OutputStreamWriter osw = new OutputStreamWriter(gos, StandardCharsets.UTF_8);
                     BufferedWriter writer = new BufferedWriter(osw)) {

                    for (CommentEntry entry : entries) {
                        writer.write(entry.toJson().toString());
                        writer.newLine();
                    }
                }
            }
            dirty = false;
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to save CommentChunk", e);
        }
    }

    private static SecretKey deriveKey(ServerWorldMeta worldMeta) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(worldMeta.worldId.getMostSignificantBits() ^ 0xD6B9F48E74114514L);
        buffer.putLong(worldMeta.worldId.getLeastSignificantBits() ^ 0x58D2302460114514L);
        return new SecretKeySpec(buffer.array(), "AES");
    }
}
