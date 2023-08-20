package cn.zbx1425.worldcomment.data.sync;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.util.Map;

//Todo: Notice that this class is COMPLETELY SYNC currently
public class RedisChannelInterface {
    public RedisChannelMessageQueue Queue = new RedisChannelMessageQueue();
    private final StatefulRedisPubSubConnection<String, String> channel;

    private final StatefulRedisConnection<String, String> instance;

    private volatile boolean StartedListening = false;

    public RedisChannelInterface(String URI) {
        this.channel = RedisClient.create(URI).connectPubSub();
        this.instance = RedisClient.create(URI).connect();
    }

    public String get(String Key) {
        return this.instance.sync().get(Key);
    }

    public void set(String Key, String Value) {
        this.instance.sync().set(Key, Value);
    }

    public void hset(String Key, Map<String, String> Value) {
        this.instance.sync().hset(Key, Value);
    }

    public void hset(String Key, String MapKey, String Value) {
        this.instance.sync().hset(Key, MapKey, Value);
    }

    public Map<String, String> hgetall(String Key) {
        return this.instance.sync().hgetall(Key);
    }

    public void publish(String Channel, String Data) {
        this.channel.sync().publish(Channel, Data);
    }

    public void del(String Key) {
        this.instance.sync().del(Key);
    }

    public void hdel(String Key, String Field) {
        this.instance.sync().hdel(Key, Field);
    }

    public synchronized void recvChannel(String[] Channels) {
        if (StartedListening) {
            this.stop();
        }

        /*RedisPubSubAdapter<String, String> adapter = new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                Queue.append(channel, message);
            }
        };

        RedisPubSubAsyncCommands<String, String> command = this.channel.async();

        command.subscribe(Channels);

        this.channel.addListener(adapter);

        while (StartedListening) {
            Thread.onSpinWait();
        }

        this.channel.removeListener(adapter);*/
    }

    public void stop() {
        this.StartedListening = false;
    }

    public void close() {
        this.channel.close();
        this.instance.close();
    }

}
