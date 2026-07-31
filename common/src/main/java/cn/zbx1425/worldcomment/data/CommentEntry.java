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

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class CommentEntry {

    public static int REGION_SHIFT = 2;
    public static final int MESSAGE_MAX_LENGTH = 256;

    public static UUID SYSTEM_MESSAGE_MAGIC_INITIATOR = UUID.fromString("21b8b366-0adf-44ef-b93c-c6ec8377fa26");

    public long id;
    public long timestamp;
    public Identifier level;
    public ChunkPos region;
    public BlockPos location;
    public UUID initiator;
    public String initiatorName;
    public int messageType;
    public String message;
    public CommentImage image;

    public boolean deleted;
    public int like;

    public CommentEntry(Player initiator, boolean isAnonymous, int messageType, String message) {
        id = ServerWorldData.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        level = initiator #if MC_VERSION >= "12000" .level() #else .level #endif .dimension().identifier();
        this.initiator = initiator.getGameProfile().id();
        if (isAnonymous) {
            initiatorName = "";
        } else {
            initiatorName = initiator.getGameProfile().name();
        }
        this.messageType = messageType;
        this.message = message;
        deleted = false;
    }

    public CommentEntry(Identifier level, FriendlyByteBuf src) {
        deleted = src.readBoolean();
        src.skipBytes(3);
        like = src.readInt();
        src.skipBytes(8);

        id = src.readLong();
        timestamp = src.readLong();
        this.level = level;
        location = src.readBlockPos();
        region = new ChunkPos(location.getX() >> (4 + REGION_SHIFT), location.getZ() >> (4 + REGION_SHIFT));
        initiator = src.readUUID();
        initiatorName = src.readUtf();
        messageType = src.readInt();
        message = src.readUtf();
        image = new CommentImage(src.readUtf(), src.readUtf(), src.readUtf(), src.readUtf());
    }

    private CommentEntry() {
    }

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
        dst.writeZero(3);
        dst.writeInt(like);
        dst.writeBytes("====ZBX=".getBytes(StandardCharsets.UTF_8));

        dst.writeLong(id);
        dst.writeLong(timestamp);
        dst.writeBlockPos(location);
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

    public static CommentEntry fromJson(JsonObject json) {
        CommentEntry entry = new CommentEntry();
        entry.id = json.get("id").getAsLong();
        entry.timestamp = json.get("timestamp").getAsLong();
#if MC_VERSION >= "12100"
        entry.level = Identifier.parse(json.get("level").getAsString());
#else
        entry.level = new Identifier(json.get("level").getAsString());
#endif
        JsonArray loc = json.getAsJsonArray("location");
        entry.setLocation(new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt()));
        entry.initiator = UUID.fromString(json.get("initiator").getAsString());
        entry.initiatorName = json.get("initiatorName").getAsString();
        entry.messageType = json.get("messageType").getAsInt();
        entry.message = json.get("message").getAsString();
        if (json.has("image")) {
            entry.image = new CommentImage(json.getAsJsonObject("image"));
        } else {
            entry.image = CommentImage.NONE;
        }
        entry.deleted = json.has("deleted") && json.get("deleted").getAsBoolean();
        entry.like = json.has("like") ? json.get("like").getAsInt() : 0;
        return entry;
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
