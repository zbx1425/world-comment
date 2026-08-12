package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.uplink.UplinkDispatcher;
import cn.zbx1425.worldcomment.data.persist.CommentRepository;
import cn.zbx1425.worldcomment.data.sync.Synchronizer;
import cn.zbx1425.worldcomment.network.PacketEntryUpdateS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

public class ServerWorldData {

    public static Snowflake SNOWFLAKE = new Snowflake();

    public final MinecraftServer server;
    public final Path basePath;

    public boolean isHost;
    public ServerWorldMeta metadata;

    public final CommentStore comments = new CommentStore();

    public final CommentRepository repository;
    public final UplinkDispatcher uplinkDispatcher;
    public Synchronizer peerChannel;

    public ServerWorldData(MinecraftServer server, boolean isHost) {
        this.server = server;
        this.basePath = Path.of(server.getWorldPath(LevelResource.ROOT).toString(), "worldcomment");
        repository = new CommentRepository(basePath);
        this.isHost = isHost;
        this.peerChannel = Synchronizer.NOOP;
        uplinkDispatcher = new UplinkDispatcher(Main.SERVER_CONFIG.uplinkUrl.value);
    }

    public void load() throws IOException {
        if (isHost) {
            metadata = repository.loadInto(comments);
            peerChannel.kvWriteAll(comments.timeIndex, metadata);
        } else {
            metadata = peerChannel.kvReadAllInto(comments);
        }
    }

    public void save() {
        if (isHost) {
            repository.saveDirtyChunks(comments, metadata);
        }
    }

    public void insert(CommentEntry newEntry, boolean fromPeer) {
        if (CommentCommand.isCommand(newEntry)) {
            if (isHost) {
                CommentCommand.executeCommandServer(newEntry, this);
            }
            return;
        }
        comments.insert(newEntry);
        if (isHost) {
            uplinkDispatcher.insert(newEntry);
        }
        if (!fromPeer) {
            peerChannel.kvWriteEntry(newEntry);
            peerChannel.notifyInsert(newEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, newEntry, false);
        }
    }

    public void update(CommentEntry newEntry, boolean fromPeer) {
        CommentEntry trustedEntry = comments.update(newEntry);
        if (trustedEntry == null) return;
        if (isHost) {
            uplinkDispatcher.update(trustedEntry);
        }
        if (!fromPeer) {
            peerChannel.kvWriteEntry(trustedEntry);
            peerChannel.notifyUpdate(trustedEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, trustedEntry, true);
        }
    }

    public void updateAllFields(CommentEntry newEntry, boolean fromPeer) {
        comments.updateAllFields(newEntry);
        if (isHost) {
            uplinkDispatcher.update(newEntry);
        }
        if (!fromPeer) {
            peerChannel.kvWriteEntry(newEntry);
            peerChannel.notifyUpdateAllFields(newEntry);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketEntryUpdateS2C.send(player, newEntry, true);
        }
    }
}
