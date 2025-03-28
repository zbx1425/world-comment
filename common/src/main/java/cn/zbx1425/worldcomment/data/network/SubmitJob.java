package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploadConfig;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SubmitJob {

    public final CommentEntry comment;
    public final byte[] imageBytes;
    public boolean imageReady, blockPosReady;
    public BiConsumer<SubmitJob, Throwable> callback;
    public Queue<ImageUploader> uploaderToUse;

    public SubmitJob(CommentEntry comment, byte[] imageBytes, BiConsumer<SubmitJob, Throwable> callback, ClientConfig config) {
        this.comment = comment;
        this.imageBytes = imageBytes;
        if (imageBytes == null) {
            comment.image = ThumbImage.NONE;
            imageReady = true;
        }
        this.callback = callback;
        this.uploaderToUse = new LinkedList<>(config.imageUploader);
    }

    public void setImage(ThumbImage image) {
        comment.image = image;
        imageReady = true;
    }

    public void setLocation(BlockPos blockPos) {
        comment.setLocation(blockPos);
        blockPosReady = true;
    }

    public boolean isReady() {
        return imageReady && blockPosReady;
    }

}
