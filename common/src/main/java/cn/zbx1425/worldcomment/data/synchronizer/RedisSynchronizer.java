package cn.zbx1425.worldcomment.data.synchronizer;

import cn.zbx1425.third_party.data.redis.RedisChannelInterface;
import cn.zbx1425.worldcomment.data.CommentEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RedisSynchronizer implements Synchronizer {

    private final RedisChannelInterface redis;

    private final boolean host;
    public RedisSynchronizer(String URI, boolean host) {
        this.redis = new RedisChannelInterface(URI);
        this.host = host;
    }

    @Override
    public void sync(Path path) throws IOException {
        if (this.host) {
            this.upload(path);
        } else {
            this.fetchAll(path);
        }
    }


    @Override
    public void update(CommentEntry entry, Path targetFile) throws IOException {
        try (RandomAccessFile oStream = new RandomAccessFile(targetFile.toFile(), "rw")) {
            entry.updateInFile(oStream);
        }


        if (this.host) {
            this.redis.hset(
                    Command.DataKey(""),
                    new HashMap<String, String>(){{
                        put(String.valueOf(entry.id), entry.toString());
                    }});


            this.redis.publish(
                    Command.COMMAND_CHANNEL,
                    Command.Updated(String.valueOf(entry.id)));

        } else {
            this.redis.set(Command.DataKey(
                            String.valueOf(entry.id)),
                    entry.toString());

            this.redis.publish(
                    Command.COMMAND_CHANNEL,
                    Command.Update(String.valueOf(entry.id)));
        }
    }

    private void upload(Path dirPath) throws IOException {
        HashMap<String, String> pendingUpload = new HashMap<>();

        try {
            Files.createDirectories(dirPath);
        } catch (FileAlreadyExistsException ignored) {

        }
        try (Stream<Path> levelFiles = Files.list(dirPath)) {
            for (Path levelPath : levelFiles.toList()) {
                ResourceLocation dimension = new ResourceLocation(levelPath.getFileName().toString().replace("+", ":"));
                try (Stream<Path> files = Files.list(levelPath)) {
                    for (Path file : files.toList()) {
                        String[] fileNameParts = file.getFileName().toString().split("\\.");
                        if (fileNameParts.length != 4 || !fileNameParts[3].equals("bin")) continue;
                        byte[] data = Files.readAllBytes(file);
                        FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                        while (src.readerIndex() < data.length) {
                            CommentEntry entry = new CommentEntry(dimension, src, false);
                            pendingUpload.put(String.valueOf(entry.id), entry.toString());
                        }
                    }
                }
            }
        }

        this.redis.hset(
                Command.DataKey(""),
                pendingUpload
        );

    }

    private Map<String, String> fetchAll(Path dirPath) throws IOException {
        Map<String, String> exist = this.redis.hgetall(Command.DataKey(""));

        try {
            Files.createDirectories(dirPath);
        } catch (FileAlreadyExistsException ignored) {

        }
        try (Stream<Path> levelFiles = Files.list(dirPath)) {
            for (Path levelPath : levelFiles.toList()) {
                ResourceLocation dimension = new ResourceLocation(levelPath.getFileName().toString().replace("+", ":"));
                try (Stream<Path> files = Files.list(levelPath)) {
                    for (Path file : files.toList()) {
                        String[] fileNameParts = file.getFileName().toString().split("\\.");
                        if (fileNameParts.length != 4 || !fileNameParts[3].equals("bin")) continue;
                        byte[] data = Files.readAllBytes(file);
                        FriendlyByteBuf src = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                        while (src.readerIndex() < data.length) {
                            CommentEntry entry = new CommentEntry(dimension, src, false);
                            exist.remove(String.valueOf(entry.id));
                        }
                    }
                }
            }
        }

        exist.forEach((id, entry) -> {
            CommentEntry commentEntry = new CommentEntry();
            this.update(commentEntry, getLevelRegionPath(commentEntry.level, commentEntry.region));
        });


        //cache cleaned or some other situation
        if (false) {
            sendFetchCommand();
        }
    }

    private void sendFetchCommand() {
        this.redis.publish(
                Command.COMMAND_CHANNEL,
                Command.Request("")
        );
    }

    public class receiver extends Thread {
        @Override
        public void run() {
            redis.recvChannel(new String[]{Command.COMMAND_CHANNEL});

            //todo: set queue handler will be better?
            while (true) {
                String command = redis.Queue.next();

                if (command.) {
                    continue;
                }
            }

        }
    }

}
