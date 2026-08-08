package cn.zbx1425.worldcomment.data.network.upload;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.NonNull;

import java.net.URI;

public record CdnTransformConfig(@NonNull String template) {

    public boolean isEnabled() {
        return !template.isEmpty();
    }

    public String apply(URI sourceUrl, ImageFilePurpose variant, ImageVariantConfig.VariantSpec variantSpec) {
        if (!isEnabled()) return sourceUrl.toString();
        return UrlTemplate.transformCdn(template, sourceUrl, variant, variantSpec);
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
