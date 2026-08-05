package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.network.CommentImage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class CommentEntry {

    public static int REGION_SHIFT = 2;
    public static final int MESSAGE_MAX_LENGTH = 256;

    public static UUID SYSTEM_MESSAGE_MAGIC_INITIATOR = UUID.fromString("21b8b366-0adf-44ef-b93c-c6ec8377fa26");

    public long id;
    public long timestamp;
    public @NonNull Identifier level;
    public @Nullable ChunkPos region;
    public @Nullable BlockPos location;
    public @Nullable BlockPos imageLocation;
    public @NonNull UUID initiator;
    public @NonNull String initiatorName;
    public int messageType;
    public @NonNull String message;
    public @NonNull CommentImage image;

    public boolean deleted;
    public int like;

    public CommentEntry(Player initiator, boolean isAnonymous, int messageType, String message, BlockPos imageLocation) {
        id = ServerWorldData.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        //? if >=1.20 {
        level = initiator.level().dimension().identifier();
        //?} else {
        /*level = initiator.level.dimension().identifier();
        *///?}
        this.initiator = initiator.getGameProfile().id();
        if (isAnonymous) {
            initiatorName = "";
        } else {
            initiatorName = initiator.getGameProfile().name();
        }
        this.messageType = messageType;
        this.message = message;
        deleted = false;
        this.imageLocation = imageLocation;
    }

    // From packet
    public CommentEntry(Identifier level, FriendlyByteBuf src) {
        deleted = src.readBoolean();
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
        level = Identifier.parse(json.get("level").getAsString());
//? } else {
        /*level = new Identifier(json.get("level").getAsString());
*///? }
        if (json.has("location")) {
            JsonArray loc = json.getAsJsonArray("location");
            setLocation(new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt()));
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
        this.setLocation(BlockPos.ZERO);
        this.imageLocation = BlockPos.ZERO;
    }

    public void setLocation(BlockPos location) {
        this.location = location;
        this.region = new ChunkPos(location.getX() >> (4 + REGION_SHIFT), location.getZ() >> (4 + REGION_SHIFT));
    }

    public void copyFrom(CommentEntry other) {
        this.messageType = other.messageType;
        this.message = other.message;
        this.image = other.image;
        this.deleted = other.deleted;
        this.like = other.like;
    }

    public void writeBuffer(FriendlyByteBuf dst) {
        dst.writeBoolean(deleted);
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
        json.addProperty("like", like);
        return json;
    }

    public ByteBuf toBinaryBuffer() {
        FriendlyByteBuf dest = new FriendlyByteBuf(Unpooled.buffer(512));
        dest.writeIdentifier(level);
        writeBuffer(dest);
        return dest;
    }

    public static CommentEntry fromBinaryBuffer(ByteBuf buf) {
        FriendlyByteBuf src = new FriendlyByteBuf(buf);
        Identifier level = src.readIdentifier();
        return new CommentEntry(level, src);
    }

    public static CommentEntry createSystemMessage(int messageType, String message, String title) {
        return new CommentEntry(messageType, message, title);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
