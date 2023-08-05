package cn.zbx1425.worldcomment.data.sync;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.persist.FileSerializer;

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
    public void sync(Path path) throws IOException {
        //do nothing
    }

    @Override
    public void update(CommentEntry entry, Path targetFile) throws IOException {

    }
}
