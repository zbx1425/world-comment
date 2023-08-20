package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RedisSynchronizer implements Synchronizer {

    private final RedisChannelInterface redis;
    private final ServerWorldData serverWorldData;

    public RedisSynchronizer(String URI, ServerWorldData serverWorldData) {
        this.redis = new RedisChannelInterface(URI);
        this.serverWorldData = serverWorldData;

        receiver hook = new receiver();
        hook.start();
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all) {
        HashMap<String, String> data = new HashMap<>();
        for (CommentEntry entry : all.values()) {
            data.put(Long.toHexString(entry.id), entry.toBinaryString());
        }
        this.redis.hset(RedisCommand.HMAP_ALL_ID, data);
    }

    @Override
    public void kvWriteEntry(CommentEntry newEntry) {
        if (newEntry.deleted) {
            this.redis.hdel(RedisCommand.HMAP_ALL_ID, Long.toHexString(newEntry.id));
        } else {
            this.redis.hset(RedisCommand.HMAP_ALL_ID, Long.toHexString(newEntry.id), newEntry.toBinaryString());
        }
    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        redis.publish(RedisCommand.COMMAND_CHANNEL, RedisCommand.Insert(newEntry.toBinaryString()));
    }

    private void handleInsert(CommentEntry peerEntry) throws IOException {
        serverWorldData.insert(peerEntry, true);
    }

    @Override
    public void notifyUpdate(CommentEntry newEntry) {
        redis.publish(RedisCommand.COMMAND_CHANNEL, RedisCommand.Update(newEntry.toBinaryString()));
    }

    private void handleUpdate(CommentEntry peerEntry) throws IOException {
        serverWorldData.update(peerEntry, true);
    }

    @Override
    public void kvReadAllInto(CommentCache comments) throws IOException {
        Map<String, String> data = redis.hgetall(RedisCommand.HMAP_ALL_ID);
        for (String entry : data.values()) {
            comments.insert(CommentEntry.fromBinaryString(entry));
        }
    }

    public class receiver extends Thread {
        @Override
        public void run() {
            redis.recvChannel(new String[]{RedisCommand.COMMAND_CHANNEL});

            //todo: set queue handler will be better?
            while (true) {
                String command = redis.Queue.next();
                if (command.isEmpty()) continue;

                try {
                    switch (RedisCommand.getAction(command)) {
                        case "INSERT" -> handleInsert(CommentEntry.fromBinaryString(RedisCommand.getContent(command)));
                        case "UPDATE" -> handleUpdate(CommentEntry.fromBinaryString(RedisCommand.getContent(command)));
                    }
                } catch (IOException ex) {
                    Main.LOGGER.error("Redis handler", ex);
                }
            }

        }
    }

}
