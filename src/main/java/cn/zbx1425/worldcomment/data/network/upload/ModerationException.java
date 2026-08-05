package cn.zbx1425.worldcomment.data.network.upload;

import java.io.IOException;

public class ModerationException extends IOException {

    private final Code code;

    public ModerationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        MODERATION_REJECTED,
        MODERATION_ERROR
    }
}
