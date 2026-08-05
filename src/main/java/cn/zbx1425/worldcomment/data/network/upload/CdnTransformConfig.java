package cn.zbx1425.worldcomment.data.network.upload;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.NonNull;

import java.net.URI;

public record CdnTransformConfig(@NonNull String template) {

    public boolean isEnabled() {
        return !template.isEmpty();
    }

    public String apply(String sourceUrl, int width, int quality) {
        if (!isEnabled()) return sourceUrl;
        try {
            URI uri = URI.create(sourceUrl);
            String path = uri.getPath();
            String pathNoLeadingSlash = path.startsWith("/") ? path.substring(1) : path;
            String transformed = template
                .replace("{path}", pathNoLeadingSlash)
                .replace("{width}", Integer.toString(width))
                .replace("{quality}", Integer.toString(quality))
                .replace("{quality_frac}", String.format("%.2f", quality / 100f));
            return sourceUrl.replace(path, transformed);
        } catch (Exception e) {
            return sourceUrl;
        }
    }

    public static CdnTransformConfig fromJson(JsonObject uploaderConfig) {
        String template = uploaderConfig.has("cdnImageTransform")
            ? uploaderConfig.get("cdnImageTransform").getAsString() : "";
        return new CdnTransformConfig(template);
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeUtf(template);
    }

    public static CdnTransformConfig readPacket(FriendlyByteBuf buf) {
        String template = buf.readUtf();
        return new CdnTransformConfig(template);
    }
}
