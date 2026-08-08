package cn.zbx1425.worldcomment.data.network.upload;

import net.minecraft.network.chat.Component;

public record ModerationWarning(Code code, String detail) implements UploadOutcome.Warning {

    public enum Code {
        MODERATION_REJECTED,
        MODERATION_ERROR
    }

    @Override
    public Component message() {
        return switch (code) {
            case MODERATION_ERROR ->
                Component.translatable("gui.worldcomment.upload_warning.moderation_error", detail);
            case MODERATION_REJECTED ->
                Component.translatable("gui.worldcomment.upload_warning.moderation_rejected", detail);
        };
    }
}
