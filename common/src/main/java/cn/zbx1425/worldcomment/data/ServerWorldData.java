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
    public final RedisSynchronizer synchronizer;

    public ServerWorldData(MinecraftServer server) {
        this.server = server;
        this.basePath = Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "world-comment");
        fileSerializer = new FileSerializer(basePath);
        //Todo: sync config inject
        this.synchronizer = new RedisSynchronizer("", true, basePath)
    }

    public void load() throws IOException {
        if (isHost) {
            fileSerializer.loadInto(comments);
            synchronizer.kvWriteAll(comments.timeIndex);
        } else {
            synchronizer.kvReadAllInto(comments);
        }
    }

    public void insert(CommentEntry newEntry, boolean fromPeer) throws IOException {
        comments.insert(newEntry);
        if (isHost) {
            fileSerializer.insert(newEntry);
            synchronizer.kvWriteEntry(newEntry);
        }
        if (!fromPeer) {
            synchronizer.notifyInsert(newEntry);
        }
    }

    public void update(CommentEntry newEntry, boolean fromPeer) throws IOException {
        CommentEntry trustedEntry = comments.update(newEntry);
        if (isHost) {
            fileSerializer.update(trustedEntry);
            synchronizer.kvWriteEntry(trustedEntry);
        }
        if (!fromPeer) {
            synchronizer.notifyUpdate(trustedEntry);
        }
    }
}
