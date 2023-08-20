package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

public class NoopSynchronizer implements Synchronizer {

    public NoopSynchronizer() {

    }

    @Override
    public void kvWriteEntry(CommentEntry trustedEntry) {

    }

    @Override
    public void notifyUpdate(CommentEntry trustedEntry) {

    }

    @Override
    public void notifyInsert(CommentEntry newEntry) {

    }

    @Override
    public void kvReadAllInto(CommentCache comments) {

    }

    @Override
    public void kvWriteAll(Long2ObjectSortedMap<CommentEntry> timeIndex) {

    }

    @Override
    public void close() {

    }
}
