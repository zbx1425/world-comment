package cn.zbx1425.worldcomment.data.network;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.NonNull;

public class CommentImage {

    public final @NonNull String uploaderId;
    public final @NonNull String sourceUrl;
    public final @NonNull String mediumUrl;
    public final @NonNull String thumbUrl;

    public static final CommentImage NONE = new CommentImage("", "", "", "");

    public CommentImage(@NonNull String uploaderId, @NonNull String sourceUrl,
                        @NonNull String mediumUrl, @NonNull String thumbUrl) {
        this.uploaderId = uploaderId;
        this.sourceUrl = sourceUrl;
        this.mediumUrl = mediumUrl;
        this.thumbUrl = thumbUrl;
    }

    public CommentImage(JsonObject json) {
        this.uploaderId = json.has("uploader") ? json.get("uploader").getAsString() : "";
        this.sourceUrl = json.has("src") ? json.get("src").getAsString() : "";
        this.mediumUrl = json.has("mid") ? json.get("mid").getAsString() : "";
        this.thumbUrl = json.has("thumb") ? json.get("thumb").getAsString() : "";
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uploader", uploaderId);
        json.addProperty("src", sourceUrl);
        if (!mediumUrl.isEmpty()) json.addProperty("mid", mediumUrl);
        if (!thumbUrl.isEmpty()) json.addProperty("thumb", thumbUrl);
        return json;
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeUtf(uploaderId);
        buf.writeUtf(sourceUrl);
        buf.writeUtf(mediumUrl);
        buf.writeUtf(thumbUrl);
    }

    public static CommentImage readPacket(FriendlyByteBuf buf) {
        return new CommentImage(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }
}
