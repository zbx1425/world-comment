package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.persist.FileSerializer;
import com.mojang.datafixers.TypeRewriteRule;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RedisSynchronizer implements Synchronizer {

    private final RedisChannelInterface redis;

    private final FileSerializer Serializer;

    private final boolean host;

    public RedisSynchronizer(String URI, Path persist, boolean host) {
        this.redis = new RedisChannelInterface(URI);
        this.Serializer = new FileSerializer(persist);
        this.host = host;

        receiver hook = new receiver();
        hook.start();
    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> all) {
        HashMap<String, String> data = new HashMap<>();

        for (CommentEntry entry : all.values()) {
            data.put(String.valueOf(entry.id), entry.toJson());
        }

        if (data.isEmpty()) {
            return;
        }

        this.redis.hset(
                Command.DataKey(
                        Command.ALL_DATA_ID
                ),
                data
        );

    }


    @Override
    public void kvWriteEntry(CommentEntry trustedEntry) {
        HashMap<String, String> data = new HashMap<>();
        data.put(String.valueOf(trustedEntry.id), trustedEntry.toJson());

        if (trustedEntry.deleted) {
            this.redis.hdel(
                    Command.DataKey(
                            Command.ALL_DATA_ID
                    ),
                    String.valueOf(trustedEntry.id)
            );
            return;
        }


        this.redis.hset(
               Command.DataKey(
                    Command.ALL_DATA_ID
               ),
               data
        );
    }

    public void notifyUpdated(CommentEntry trustedEntry) {
        redis.publish(
                Command.COMMAND_CHANNEL,
                Command.Updated(
                        String.valueOf(trustedEntry.id)
                )
        );
    }

    @Override
    public void notifyUpdate(CommentEntry trustedEntry) {
        if (!host) {
            redis.set(
                    Command.DataKey(
                            String.valueOf(trustedEntry.id)
                    ),
                    trustedEntry.toJson());

            redis.publish(
                    Command.COMMAND_CHANNEL,
                    Command.Update(
                            String.valueOf(trustedEntry.id)
                    )
            );

            return;
        }

        kvWriteEntry(trustedEntry);
        notifyUpdated(trustedEntry);

    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {
        if (!host) {
            redis.set(
                    Command.DataKey(
                            String.valueOf(newEntry.id)
                    ),
                    newEntry.toJson());


            redis.publish(
                    Command.COMMAND_CHANNEL,
                    Command.Update(
                            String.valueOf(newEntry.id)
                    )
            );

            return;
        }

        kvWriteEntry(newEntry);
        notifyUpdated(newEntry);

    }

    @Override
    public void kvReadAllInto(CommentCache comments) throws IOException {
        Map<String, String> data = redis.hgetall(
                Command.DataKey(
                        Command.ALL_DATA_ID
                )
        );

        for (String entry : data.values()) {
            CommentEntry comment = CommentEntry.fromJson(entry);
            Serializer.cover(comment, false);
        }
    }

    //Client action only
    private void onUpdated(long id) throws IOException {
        Map<String, String> rawMap = redis.hgetall(
                Command.DataKey(
                        Command.ALL_DATA_ID
                )
        );

        String raw = rawMap.get(String.valueOf(id));
        if (raw.isEmpty()) {
            return;
        }

        CommentEntry comment = CommentEntry.fromJson(raw);

        Serializer.cover(comment, true);

    }

    //Host action only
    private void onUpdate(long id) throws IOException {
        String raw = redis.get(
                Command.DataKey(
                        String.valueOf(id)
                )
        );

        CommentEntry comment = CommentEntry.fromJson(raw);

        Serializer.cover(comment, true);

        kvWriteEntry(comment);

        redis.del(
                Command.DataKey(
                        String.valueOf(id)
                ));



        redis.publish(
                Command.COMMAND_CHANNEL,
                Command.Updated(
                        String.valueOf(id)
                )
        );
    }

    public class receiver extends Thread {
        @Override
        public void run() {
            redis.recvChannel(new String[]{Command.COMMAND_CHANNEL});

            //todo: set queue handler will be better?
            while (true) {
                String command = redis.Queue.next();

                if (command.isEmpty()) {
                    continue;
                }

                if (!host) {
                    long id = Command.IsUpdated(command);
                    if (id == 0) {
                        continue;
                    }


                    try {
                        onUpdated(id);
                    } catch (IOException e) {
                        //Todo: do something
                    }
                } else {
                    long id = Command.IsUpdate(command);
                    if (id == 0) {
                        continue;
                    }

                    try {
                        onUpdate(id);
                    } catch (IOException e) {
                        //Todo: do something
                    }
                }

            }

        }
    }

}
