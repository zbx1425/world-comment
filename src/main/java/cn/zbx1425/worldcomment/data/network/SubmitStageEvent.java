package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.data.network.upload.UploadOutcome;

import java.util.List;

public sealed interface SubmitStageEvent {

    /** The submission has been completed. */
    record Sent(List<UploadOutcome.Warning> warnings) implements SubmitStageEvent {}

    /** The player has assigned a location to CommentEntry, but the uploader is still working. */
    record WaitingForUpload() implements SubmitStageEvent {}

    /** An uploader has failed to process the image. Other uploaders might succeed. */
    record UploaderFailed(Throwable cause) implements SubmitStageEvent {}
}
