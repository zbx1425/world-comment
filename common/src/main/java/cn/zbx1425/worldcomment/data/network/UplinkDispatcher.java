package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.data.CommentEntry;

public class UplinkDispatcher {

    private final String uplinkApiUrl;

    public UplinkDispatcher(String uplinkApiUrl) {
        this.uplinkApiUrl = uplinkApiUrl;
    }

    public void insert(CommentEntry commentEntry) {
        update(commentEntry);
    }

    public void update(CommentEntry commentEntry) {
        if (!uplinkApiUrl.isEmpty()) {
            new UplinkRequest(uplinkApiUrl, commentEntry.toJson()).sendAsync();
        }
    }
}
