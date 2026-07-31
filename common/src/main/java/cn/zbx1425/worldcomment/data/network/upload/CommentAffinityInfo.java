package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.CommentEntry;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class CommentAffinityInfo {

    public final @NonNull UUID initiator;
    public final @NonNull String initiatorName;
    public final @NonNull String message;

    public CommentAffinityInfo(@NonNull UUID initiator, @NonNull String initiatorName, @NonNull String message) {
        this.initiator = initiator;
        this.initiatorName = initiatorName;
        this.message = message;
    }

    public CommentAffinityInfo(@NonNull UUID initiator, @NonNull String initiatorName) {
        this(initiator, initiatorName, "");
    }

    public CommentAffinityInfo(CommentEntry comment) {
        this.initiator = comment.initiator;
        this.initiatorName = comment.initiatorName;
        this.message = comment.message;
    }
}
