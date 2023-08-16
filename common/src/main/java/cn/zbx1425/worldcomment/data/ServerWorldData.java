package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.persist.FileSerializer;
import cn.zbx1425.worldcomment.data.sync.RedisSynchronizer;
import cn.zbx1425.worldcomment.data.sync.Synchronizer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

public class ServerWorldData {

    public static final Snowflake SNOWFLAKE = new Snowflake();

    public final MinecraftServer server;
    public final Path basePath;

    public boolean isHost = true;

    public final CommentCache comments = new CommentCache();

    public final FileSerializer fileSerializer;
    public final Synchronizer synchronizer;

    public ServerWorldData(MinecraftServer server, Synchronizer synchronizer) {
        this.server = server;
        this.basePath = Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "world-comment");
        fileSerializer = new FileSerializer(basePath);
        //Todo: sync config inject
        this.synchronizer = synchronizer;
    }

    public void load() throws IOException {
        if (isHost) {
            fileSerializer.loadInto(comments);
            synchronizer.kvWriteAll(comments.timeIndex);
        } else {
            //will cover all data
            synchronizer.kvReadAllInto(comments);
        }
    }

    public void insert(CommentEntry newEntry) throws IOException {
        comments.insert(newEntry);
        fileSerializer.insert(newEntry);
        synchronizer.notifyInsert(newEntry);
    }

    public void update(CommentEntry newEntry) throws IOException {
        CommentEntry trustedEntry = comments.update(newEntry);
        fileSerializer.update(trustedEntry);
        synchronizer.notifyUpdate(newEntry);
    }
}
