package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class RedisSynchronizer implements Synchronizer {

    private final StatefulRedisPubSubConnection<String, ByteBuf> redisSub;
    private final StatefulRedisConnection<String, ByteBuf> redisConn;

    public static final String HMAP_ALL_KEY = "WORLD_COMMENT_DATA_ALL";

    private final ServerWorldData serverWorldData;

    public RedisSynchronizer(String URI, ServerWorldData serverWorldData) {
        redisConn = RedisClient.create(URI).connect(ByteBufCodec.INSTANCE);
        redisSub = RedisClient.create(URI).connectPubSub(ByteBufCodec.INSTANCE);
        redisSub.addListener(new Listener());
        redisSub.sync().subscribe(RedisMessage.COMMAND_CHANNEL);

        this.serverWorldData = serverWorldData;
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all) {
        RedisAsyncCommands<String, ByteBuf> commands = redisConn.async();
        commands.multi();
        commands.del(HMAP_ALL_KEY);
        HashMap<String, ByteBuf> data = new HashMap<>();
        for (CommentEntry entry : all.values()) {
            data.put(Long.toHexString(entry.id), entry.toBinaryBuffer());
        }
        commands.hset(HMAP_ALL_KEY, data);
        commands.exec();
    }

    @Override
    public void kvWriteEntry(CommentEntry newEntry) {
        if (newEntry.deleted) {
            redisConn.async().hdel(HMAP_ALL_KEY, Long.toHexString(newEntry.id));
        } else {
            redisConn.async().hset(HMAP_ALL_KEY, Long.toHexString(newEntry.id), newEntry.toBinaryBuffer());
        }
    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        RedisMessage.insert(newEntry).publishAsync(redisConn);
    }

    protected void handleInsert(CommentEntry peerEntry) throws IOException {
        serverWorldData.insert(peerEntry, true);
    }

    @Override
    public void notifyUpdate(CommentEntry newEntry) {
        RedisMessage.update(newEntry).publishAsync(redisConn);
    }

    protected void handleUpdate(CommentEntry peerEntry) throws IOException {
        serverWorldData.update(peerEntry, true);
    }

    @Override
    public void kvReadAllInto(CommentCache comments) throws IOException {
        Map<String, ByteBuf> data = redisConn.sync().hgetall(HMAP_ALL_KEY);
        for (ByteBuf entry : data.values()) {
            comments.insert(CommentEntry.fromBinaryBuffer(entry));
        }
    }

    @Override
    public void close() {
        redisSub.close();
        redisConn.close();
    }

    public class Listener implements RedisPubSubListener<String, ByteBuf> {
        @Override
        public void message(String channel, ByteBuf rawMessage) {
            RedisMessage message = new RedisMessage(rawMessage);
            try {
                message.handle(RedisSynchronizer.this);
            } catch (IOException ex) {
                Main.LOGGER.error("Redis handler", ex);
            }
        }

        @Override
        public void message(String pattern, String channel, ByteBuf message) { }

        @Override
        public void subscribed(String channel, long count) { }

        @Override
        public void psubscribed(String pattern, long count) { }

        @Override
        public void unsubscribed(String channel, long count) { }

        @Override
        public void punsubscribed(String pattern, long count) { }
    }

    private static class ByteBufCodec implements RedisCodec<String, ByteBuf> {

        public static ByteBufCodec INSTANCE = new ByteBufCodec();

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StringCodec.UTF8.decodeKey(bytes);
        }

        @Override
        public ByteBuf decodeValue(ByteBuffer bytes) {
            ByteBuf result = Unpooled.buffer(bytes.remaining());
            result.writeBytes(bytes);
            return result;
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StringCodec.UTF8.encodeKey(key);
        }

        @Override
        public ByteBuffer encodeValue(ByteBuf value) {
            return value.nioBuffer();
        }
    }
}
