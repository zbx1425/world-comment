package cn.zbx1425.worldcomment.data.network.upload;

import cn.zbx1425.worldcomment.data.network.CommentImage;
import net.minecraft.network.chat.Component;

import java.util.List;

public record UploadOutcome(CommentImage image, List<Warning> warnings) {

    public interface Warning {
        Component message();
    }

    public static UploadOutcome success(CommentImage image) {
        return new UploadOutcome(image, List.of());
    }
}
