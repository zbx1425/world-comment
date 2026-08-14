package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.UploadOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

public class SubmitJob {

    public final CommentEntry comment;
    public final byte[] imageBytes;
    public boolean imageReady, blockPosReady;
    public Consumer<SubmitStageEvent> callback;
    public Queue<ImageUploader> uploaderToUse;

    private final List<UploadOutcome.Warning> uploadWarnings = new ArrayList<>();

    public SubmitJob(CommentEntry comment, byte[] imageBytes, Consumer<SubmitStageEvent> callback, ClientConfig config) {
        this.comment = comment;
        this.imageBytes = imageBytes;
        if (imageBytes == null) {
            comment.image = CommentImage.NONE;
            imageReady = true;
        }
        this.callback = callback;
        this.uploaderToUse = new LinkedList<>(config.serverIssuedConfig.imageUploaders);
    }

    public void acceptUploadOutcome(UploadOutcome outcome) {
        comment.image = outcome.image();
        uploadWarnings.addAll(outcome.warnings());
        imageReady = true;
    }

    public List<UploadOutcome.Warning> uploadWarnings() {
        return Collections.unmodifiableList(uploadWarnings);
    }

    public void setLocation(Identifier level, BlockPos blockPos) {
        comment.setLocation(level, blockPos);
        blockPosReady = true;
    }

    public boolean isReady() {
        return imageReady && blockPosReady;
    }
}
