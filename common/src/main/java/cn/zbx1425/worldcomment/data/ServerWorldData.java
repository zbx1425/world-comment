package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.persist.FileSerializer;
import cn.zbx1425.worldcomment.data.sync.Synchronizer;
import cn.zbx1425.worldcomment.network.PacketEntryUpdateS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
    public Synchronizer peerChannel;

    public ServerWorldData(MinecraftServer server) {
        this.server = server;
        this.basePath = Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "world-comment");
        fileSerializer = new FileSerializer(basePath);
        this.peerChannel = Synchronizer.NOOP;
    }

    public void load() throws IOException {
        if (isHost) {
            fileSerializer.loadInto(comments);
            peerChannel.kvWriteAll(comments.timeIndex);
        } else {
            peerChannel.kvReadAllInto(comments);
        }
    }

    public void insert(CommentEntry newEntry, boolean fromPeer) throws IOException {
        comments.insert(newEntry);
        if (isHost) {
            fileSerializer.insert(newEntry);
            peerChannel.kvWriteEntry(newEntry);
        }
        if (!fromPeer) {
            peerChannel.notifyInsert(newEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, newEntry, false);
        }
    }

    public void update(CommentEntry newEntry, boolean fromPeer) throws IOException {
        CommentEntry trustedEntry = comments.update(newEntry);
        if (isHost) {
            fileSerializer.update(trustedEntry);
            peerChannel.kvWriteEntry(trustedEntry);
        }
        if (!fromPeer) {
            peerChannel.notifyUpdate(trustedEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, trustedEntry, true);
        }
    }

    public void updateUplinkState(CommentEntry newEntry) throws IOException {
        CommentEntry trustedEntry = comments.update(newEntry);
        if (isHost) {
            fileSerializer.update(trustedEntry);
        } else {
            throw new AssertionError("updateUplinkState called at non-host peer");
        }
    }
}
