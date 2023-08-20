package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RedisSynchronizer implements Synchronizer {

    private final StatefulRedisPubSubConnection<String, String> redisChannel;
    private final StatefulRedisConnection<String, String> redisConn;

    private final ServerWorldData serverWorldData;

    public RedisSynchronizer(String URI, ServerWorldData serverWorldData) {
        redisConn = RedisClient.create(URI).connect();
        redisChannel = RedisClient.create(URI).connectPubSub();
        redisChannel.addListener(new Listener());
        redisChannel.sync().subscribe(RedisCommand.COMMAND_CHANNEL);

        this.serverWorldData = serverWorldData;
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all) {
        HashMap<String, String> data = new HashMap<>();
        for (CommentEntry entry : all.values()) {
            data.put(Long.toHexString(entry.id), entry.toBinaryString());
        }
        redisConn.async().hset(RedisCommand.HMAP_ALL_ID, data);
    }

    @Override
    public void kvWriteEntry(CommentEntry newEntry) {
        if (newEntry.deleted) {
            redisConn.async().hdel(RedisCommand.HMAP_ALL_ID, Long.toHexString(newEntry.id));
        } else {
            redisConn.async().hset(RedisCommand.HMAP_ALL_ID, Long.toHexString(newEntry.id), newEntry.toBinaryString());
        }
    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        redisChannel.async().publish(RedisCommand.COMMAND_CHANNEL, RedisCommand.Insert(newEntry.toBinaryString()));
    }

    private void handleInsert(CommentEntry peerEntry) throws IOException {
        serverWorldData.insert(peerEntry, true);
    }

    @Override
    public void notifyUpdate(CommentEntry newEntry) {
        redisChannel.async().publish(RedisCommand.COMMAND_CHANNEL, RedisCommand.Update(newEntry.toBinaryString()));
    }

    private void handleUpdate(CommentEntry peerEntry) throws IOException {
        serverWorldData.update(peerEntry, true);
    }

    @Override
    public void kvReadAllInto(CommentCache comments) throws IOException {
        Map<String, String> data = redisChannel.sync().hgetall(RedisCommand.HMAP_ALL_ID);
        for (String entry : data.values()) {
            comments.insert(CommentEntry.fromBinaryString(entry));
        }
    }

    @Override
    public void close() {
        redisChannel.close();
        redisConn.close();
    }

    public class Listener implements RedisPubSubListener<String, String> {
        @Override
        public void message(String channel, String message) {
            try {
                switch (RedisCommand.getAction(message)) {
                    case "INSERT" -> handleInsert(CommentEntry.fromBinaryString(RedisCommand.getContent(message)));
                    case "UPDATE" -> handleUpdate(CommentEntry.fromBinaryString(RedisCommand.getContent(message)));
                }
            } catch (IOException ex) {
                Main.LOGGER.error("Redis handler", ex);
            }
        }

        @Override
        public void message(String pattern, String channel, String message) { }

        @Override
        public void subscribed(String channel, long count) { }

        @Override
        public void psubscribed(String pattern, long count) { }

        @Override
        public void unsubscribed(String channel, long count) { }

        @Override
        public void punsubscribed(String pattern, long count) { }
    }

}
