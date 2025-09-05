package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.util.UUID;

public class CommentAffinityInfo {

    public final UUID initiator;
    public final String initiatorName;

    public CommentAffinityInfo(UUID initiator, String initiatorName) {
        this.initiator = initiator;
        this.initiatorName = initiatorName;
    }

    public CommentAffinityInfo(CommentEntry comment) {
        this.initiator = comment.initiator;
        this.initiatorName = comment.initiatorName;
    }
}
