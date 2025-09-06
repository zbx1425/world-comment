package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldMeta;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;

public interface Synchronizer extends AutoCloseable {

    Synchronizer NOOP = new NoopSynchronizer();

    void kvWriteEntry(CommentEntry trustedEntry);

    void notifyUpdate(CommentEntry trustedEntry);

    void notifyUpdateAllFields(CommentEntry trustedEntry);

    void notifyInsert(CommentEntry newEntry);

    ServerWorldMeta kvReadAllInto(CommentCache comments) throws IOException;

    void kvWriteAll(Long2ObjectSortedMap<CommentEntry> timeIndex, ServerWorldMeta metadata);
}
