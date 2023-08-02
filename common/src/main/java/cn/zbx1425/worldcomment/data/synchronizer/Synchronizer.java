package cn.zbx1425.worldcomment.data.synchronizer;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.io.IOException;
import java.nio.file.Path;

public interface Synchronizer {

    void sync(Path path) throws IOException;

    void update(CommentEntry newEntry, Path targetFile) throws IOException;
}
