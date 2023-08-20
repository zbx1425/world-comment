package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RedisSynchronizer implements Synchronizer {

    private final StatefulRedisPubSubConnection<String, String> redisSub;
    private final StatefulRedisConnection<String, String> redisConn;

    public static final String HMAP_ALL_KEY = "WORLD_COMMENT_DATA_ALL";

    private final ServerWorldData serverWorldData;

    public RedisSynchronizer(String URI, ServerWorldData serverWorldData) {
        redisConn = RedisClient.create(URI).connect();
        redisSub = RedisClient.create(URI).connectPubSub();
        redisSub.addListener(new Listener());
        redisSub.sync().subscribe(RedisMessage.COMMAND_CHANNEL);

        this.serverWorldData = serverWorldData;
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all) {
        RedisAsyncCommands<String, String> commands = redisConn.async();
        commands.multi();
        commands.del(HMAP_ALL_KEY);
        HashMap<String, String> data = new HashMap<>();
        for (CommentEntry entry : all.values()) {
            data.put(Long.toHexString(entry.id), entry.toBinaryString());
        }
        commands.hset(HMAP_ALL_KEY, data);
        commands.exec();
    }

    @Override
    public void kvWriteEntry(CommentEntry newEntry) {
        if (newEntry.deleted) {
            redisConn.async().hdel(HMAP_ALL_KEY, Long.toHexString(newEntry.id));
        } else {
            redisConn.async().hset(HMAP_ALL_KEY, Long.toHexString(newEntry.id), newEntry.toBinaryString());
        }
    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        RedisMessage.insert(newEntry).publishAsync(redisConn);
    }

    private void handleInsert(CommentEntry peerEntry) throws IOException {
        serverWorldData.insert(peerEntry, true);
    }

    @Override
    public void notifyUpdate(CommentEntry newEntry) {
        RedisMessage.update(newEntry).publishAsync(redisConn);
    }

    private void handleUpdate(CommentEntry peerEntry) throws IOException {
        serverWorldData.update(peerEntry, true);
    }

    @Override
    public void kvReadAllInto(CommentCache comments) throws IOException {
        Map<String, String> data = redisConn.sync().hgetall(HMAP_ALL_KEY);
        for (String entry : data.values()) {
            comments.insert(CommentEntry.fromBinaryString(entry));
        }
    }

    @Override
    public void close() {
        redisSub.close();
        redisConn.close();
    }

    public class Listener implements RedisPubSubListener<String, String> {
        @Override
        public void message(String channel, String rawMessage) {
            RedisMessage message = new RedisMessage(rawMessage);
            if (message.isFromSelf()) return;
            try {
                switch (message.action) {
                    case "INSERT" -> handleInsert(CommentEntry.fromBinaryString(message.content));
                    case "UPDATE" -> handleUpdate(CommentEntry.fromBinaryString(message.content));
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
