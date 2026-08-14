package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.client.CommentPrefillInfo;
import cn.zbx1425.worldcomment.data.network.CommentImage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class CommentEntry {

    public static int REGION_SHIFT = 2;
    public static final int MESSAGE_MAX_LENGTH = 256;
    public static final int HIGH_EMOJI_BASE_ID = 65472;

    public static boolean isMarkerType(int messageType) {
        return messageType >= HIGH_EMOJI_BASE_ID;
    }

    public static UUID SYSTEM_MESSAGE_MAGIC_INITIATOR = UUID.fromString("21b8b366-0adf-44ef-b93c-c6ec8377fa26");

    public long id;
    public long timestamp;
    public @Nullable Identifier level;
    public @Nullable ChunkPos region;
    public @Nullable BlockPos location;
    public @Nullable BlockPos imageLocation;
    public @NonNull UUID initiator;
    public @NonNull String initiatorName;
    public int messageType;
    public @NonNull String message;
    public @NonNull CommentImage image;

    public boolean deleted;
    public boolean unlisted;
    public int like;

    public CommentEntry(CommentPrefillInfo prefill, boolean isAnonymous, int messageType, String message) {
        id = ServerWorldData.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        this.initiator = prefill.initiator;
        if (isAnonymous) {
            initiatorName = "";
        } else {
            initiatorName = prefill.initiatorName;
        }
        this.messageType = messageType;
        this.message = message;
        deleted = false;
        this.unlisted = prefill.unlisted;
        this.imageLocation = prefill.imageLocation;
        this.image = CommentImage.NONE;
    }

    // From packet
    public CommentEntry(Identifier level, FriendlyByteBuf src) {
        deleted = src.readBoolean();
        unlisted = src.readBoolean();
        like = src.readInt();
        id = src.readLong();
        timestamp = src.readLong();
        this.level = level;
        location = src.readBoolean() ? src.readBlockPos() : null;
        imageLocation = src.readBoolean() ? src.readBlockPos() : null;
        region = location != null
            ? new ChunkPos(location.getX() >> (4 + REGION_SHIFT), location.getZ() >> (4 + REGION_SHIFT))
            : null;
        initiator = src.readUUID();
        initiatorName = src.readUtf();
        messageType = src.readInt();
        message = src.readUtf();
        image = new CommentImage(src.readUtf(), src.readUtf(), src.readUtf(), src.readUtf());
    }

    // From JSON
    public CommentEntry(JsonObject json) {
        id = json.get("id").getAsLong();
        timestamp = json.get("timestamp").getAsLong();
//? if >=1.21 {
        Identifier level = Identifier.parse(json.get("level").getAsString());
//? } else {
        /*Identifier level = new Identifier(json.get("level").getAsString());
*///? }
        if (json.has("location")) {
            JsonArray loc = json.getAsJsonArray("location");
            setLocation(level, new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt()));
        }
        if (json.has("imageLocation")) {
            JsonArray loc = json.getAsJsonArray("imageLocation");
            imageLocation = new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt());
        }
        initiator = UUID.fromString(json.get("initiator").getAsString());
        initiatorName = json.get("initiatorName").getAsString();
        messageType = json.get("messageType").getAsInt();
        message = json.get("message").getAsString();
        if (json.has("image")) {
            image = new CommentImage(json.getAsJsonObject("image"));
        } else {
            image = CommentImage.NONE;
        }
        deleted = json.has("deleted") && json.get("deleted").getAsBoolean();
        unlisted = json.has("unlisted") && json.get("unlisted").getAsBoolean();
        like = json.has("like") ? json.get("like").getAsInt() : 0;
    }

    // For creating a system message
    private CommentEntry(int messageType, String message, String title) {
        id = ServerWorldData.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        level = Identifier.withDefaultNamespace("overworld");
        this.initiator = SYSTEM_MESSAGE_MAGIC_INITIATOR;
        this.initiatorName = title;
        this.messageType = messageType;
        this.message = message;
        deleted = false;
        this.image = CommentImage.NONE;
        this.setLocation(Level.OVERWORLD.identifier(), BlockPos.ZERO);
        this.imageLocation = BlockPos.ZERO;
    }

    public void setLocation(Identifier level, BlockPos location) {
        this.level = level;
        this.location = location;
        this.region = new ChunkPos(location.getX() >> (4 + REGION_SHIFT), location.getZ() >> (4 + REGION_SHIFT));
    }

    public void copyUpdateableFrom(CommentEntry other) {
        this.messageType = other.messageType;
        this.message = other.message;
        this.image = other.image;
        this.deleted = other.deleted;
        this.like = other.like;
    }

    public void streamWrite(FriendlyByteBuf dst) {
        dst.writeBoolean(deleted);
        dst.writeBoolean(unlisted);
        dst.writeInt(like);
        dst.writeLong(id);
        dst.writeLong(timestamp);
        dst.writeBoolean(location != null);
        if (location != null) dst.writeBlockPos(location);
        dst.writeBoolean(imageLocation != null);
        if (imageLocation != null) dst.writeBlockPos(imageLocation);
        dst.writeUUID(initiator);
        dst.writeUtf(initiatorName);
        dst.writeInt(messageType);
        dst.writeUtf(message);
        dst.writeUtf(image.uploaderId);
        dst.writeUtf(image.sourceUrl);
        dst.writeUtf(image.mediumUrl);
        dst.writeUtf(image.thumbUrl);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("timestamp", timestamp);
        json.addProperty("level", level.toString());
        if (location != null) {
            JsonArray blockPosArr = new JsonArray();
            blockPosArr.add(location.getX());
            blockPosArr.add(location.getY());
            blockPosArr.add(location.getZ());
            json.add("location", blockPosArr);
        }
        if (imageLocation != null) {
            JsonArray blockPosArr = new JsonArray();
            blockPosArr.add(imageLocation.getX());
            blockPosArr.add(imageLocation.getY());
            blockPosArr.add(imageLocation.getZ());
            json.add("imageLocation", blockPosArr);
        }
        json.addProperty("initiator", initiator.toString());
        json.addProperty("initiatorName", initiatorName);
        json.addProperty("messageType", messageType);
        json.addProperty("message", message);
        if (image != null) {
            json.add("image", image.toJson());
        }
        json.addProperty("deleted", deleted);
        json.addProperty("unlisted", unlisted);
        json.addProperty("like", like);
        return json;
    }

    public static CommentEntry createSystemMessage(int messageType, String message, String title) {
        return new CommentEntry(messageType, message, title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
