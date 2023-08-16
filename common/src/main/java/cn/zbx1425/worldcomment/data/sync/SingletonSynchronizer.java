package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentCache;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.persist.FileSerializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

//Todo: doing nothing currently
public class SingletonSynchronizer implements Synchronizer {

    private final FileSerializer Serializer;

    public SingletonSynchronizer(Path persist) {
        this.Serializer = new FileSerializer(persist);
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
}
