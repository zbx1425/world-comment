package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.*;
import com.google.gson.JsonParser;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RedisSynchronizer implements Synchronizer {

    private final RedisClient redisClient;
    private final StatefulRedisPubSubConnection<String, String> redisSub;
    private final StatefulRedisConnection<String, String> redisConn;

    public static final String HMAP_ALL_KEY = "WORLD_COMMENT_DATA_ALL";
    public static final String META_KEY = "WORLD_COMMENT_METADATA";

    private static final long META_RETRY_INTERVAL_MS = 10000;

    private final ServerWorldData serverWorldData;

    public RedisSynchronizer(String URI, ServerWorldData serverWorldData) {
        redisClient = RedisClient.create(URI);
        redisConn = redisClient.connect();
        redisSub = redisClient.connectPubSub();
        redisSub.addListener(new Listener());
        redisSub.sync().subscribe(RedisMessage.COMMAND_CHANNEL);

        this.serverWorldData = serverWorldData;
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all, ServerWorldMeta metadata) {
        RedisAsyncCommands<String, String> commands = redisConn.async();
        commands.multi();
        commands.del(HMAP_ALL_KEY);
        HashMap<String, String> data = new HashMap<>();
        for (CommentEntry entry : all.values()) {
            data.put(Long.toHexString(entry.id), entry.toJson().toString());
        }
        if (!data.isEmpty()) {
            commands.hset(HMAP_ALL_KEY, data);
        }
        commands.set(META_KEY, metadata.serialize().toString());
        checkRedisFuture(commands.exec(), "kvWriteAll");
    }

    @Override
    public void kvWriteEntry(CommentEntry newEntry) {
        checkRedisFuture(
                redisConn.async().hset(HMAP_ALL_KEY, Long.toHexString(newEntry.id), newEntry.toJson().toString()),
                "kvWriteEntry"
        );
    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        new RedisMessage(RedisMessage.Action.INSERT, newEntry.id).publishAsync(redisConn);
    }

    @Override
    public void notifyUpdate(CommentEntry newEntry) {
        new RedisMessage(RedisMessage.Action.UPDATE, newEntry.id).publishAsync(redisConn);
    }

    @Override
    public void notifyUpdateAllFields(CommentEntry newEntry) {
        new RedisMessage(RedisMessage.Action.UPDATE_ALL_FIELDS, newEntry.id).publishAsync(redisConn);
    }

    protected void handleNotification(RedisMessage.Action action, long entryId) {
        redisConn.async().hget(HMAP_ALL_KEY, Long.toHexString(entryId)).thenAccept(json -> {
            if (json == null) return; // Pruned from Redis by host restart?
            CommentEntry peerEntry = new CommentEntry(JsonParser.parseString(json).getAsJsonObject());
            serverWorldData.server.execute(() -> {
                switch (action) {
                    case INSERT:
                        if (!peerEntry.deleted && !serverWorldData.comments.containsId(entryId)) {
                            serverWorldData.insert(peerEntry, true);
                        }
                        break;
                    case UPDATE:
                        serverWorldData.update(peerEntry, true);
                        break;
                    case UPDATE_ALL_FIELDS:
                        serverWorldData.updateAllFields(peerEntry, true);
                        break;
                }
            });
        }).exceptionally(ex -> {
            Main.LOGGER.error("Failed to fetch synced comment {}", Long.toHexString(entryId), ex);
            return null;
        });
    }

    @Override
    public ServerWorldMeta kvReadAllInto(CommentStore comments) throws IOException {
        Map<String, String> data = redisConn.sync().hgetall(HMAP_ALL_KEY);
        for (String json : data.values()) {
            CommentEntry entry = new CommentEntry(JsonParser.parseString(json).getAsJsonObject());
            // Notifications may have applied some entries already; and tombstones are skipped
            if (entry.deleted || comments.containsId(entry.id)) continue;
            comments.insert(entry);
        }

        // The host may not be up yet
        while (true) {
            String metaJson = redisConn.sync().get(META_KEY);
            if (metaJson != null) {
                return new ServerWorldMeta(JsonParser.parseString(metaJson).getAsJsonObject());
            }
            Main.LOGGER.info("Waiting for the host server to publish world metadata to Redis...");
            try {
                // Keep waiting indefinitely
                // If a subordinate server starts without a working host, user might experience data loss
                Thread.sleep(META_RETRY_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for world metadata from Redis", ex);
            }
        }
    }

    @Override
    public void close() {
        redisSub.close();
        redisConn.close();
        redisClient.shutdown();
    }

    private static <T> void checkRedisFuture(RedisFuture<T> future, String operation) {
        future.whenComplete((result, ex) -> {
            if (ex != null) Main.LOGGER.error("Redis operation {} failed", operation, ex);
        });
    }

    public class Listener implements RedisPubSubListener<String, String> {
        @Override
        public void message(String channel, String rawMessage) {
            RedisMessage message = new RedisMessage(rawMessage);
            message.handle(RedisSynchronizer.this);
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
