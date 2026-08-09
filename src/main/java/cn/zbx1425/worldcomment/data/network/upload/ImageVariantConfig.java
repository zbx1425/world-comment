package cn.zbx1425.worldcomment.data.network.upload;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public record ImageVariantConfig(
    @Nullable VariantSpec archive,
    @NonNull VariantSpec detail,
    @Nullable VariantSpec thumbnail
) {

    public VariantSpec getSourceSpec() {
        return archive != null ? archive : detail;
    }

    public boolean hasArchive() {
        return archive != null;
    }

    public boolean hasThumbnail() {
        return thumbnail != null;
    }

    public static ImageVariantConfig defaults() {
        return new ImageVariantConfig(
            new VariantSpec(0, 100, true),
            new VariantSpec(1920, 95, false),
            new VariantSpec(256, 80, false)
        );
    }

    public static ImageVariantConfig fromJson(JsonObject json) {
        VariantSpec archive = json.has("archive") ? VariantSpec.fromJson(json.getAsJsonObject("archive")) : null;
        VariantSpec detail;
        if (json.has("detail")) {
            detail = VariantSpec.fromJson(json.getAsJsonObject("detail"));
        } else {
            detail = new VariantSpec(1920, 95, false);
        }
        VariantSpec thumbnail = json.has("thumbnail") ? VariantSpec.fromJson(json.getAsJsonObject("thumbnail")) : null;
        return new ImageVariantConfig(archive, detail, thumbnail);
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeBoolean(archive != null);
        if (archive != null) archive.writePacket(buf);
        detail.writePacket(buf);
        buf.writeBoolean(thumbnail != null);
        if (thumbnail != null) thumbnail.writePacket(buf);
    }

    public static ImageVariantConfig readPacket(FriendlyByteBuf buf) {
        VariantSpec archive = buf.readBoolean() ? VariantSpec.readPacket(buf) : null;
        VariantSpec detail = VariantSpec.readPacket(buf);
        VariantSpec thumbnail = buf.readBoolean() ? VariantSpec.readPacket(buf) : null;
        return new ImageVariantConfig(archive, detail, thumbnail);
    }

    public record VariantSpec(int maxWidth, int quality, boolean lossless) {

        public static VariantSpec fromJson(JsonObject json) {
            int maxWidth = json.has("maxWidth") ? json.get("maxWidth").getAsInt() : 0;
            int quality = json.has("quality") ? json.get("quality").getAsInt() : 95;
            boolean lossless = json.has("lossless") && json.get("lossless").getAsBoolean();
            return new VariantSpec(maxWidth, quality, lossless);
        }

        public void writePacket(FriendlyByteBuf buf) {
            buf.writeInt(maxWidth);
            buf.writeInt(quality);
            buf.writeBoolean(lossless);
        }

        public static VariantSpec readPacket(FriendlyByteBuf buf) {
            return new VariantSpec(buf.readInt(), buf.readInt(), buf.readBoolean());
        }
    }
}
