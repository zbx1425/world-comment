package cn.zbx1425.worldcomment.data.synchronizer;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

//Todo: doing nothing currently
public class SingletonSynchronizer implements Synchronizer {


    @Override
    public void sync(Path path) throws IOException {
        //do nothing
    }

    @Override
    public void update(CommentEntry entry, Path targetFile) throws IOException {
        try (RandomAccessFile oStream = new RandomAccessFile(targetFile.toFile(), "rw")) {
            entry.updateInFile(oStream);
        }
    }
}
