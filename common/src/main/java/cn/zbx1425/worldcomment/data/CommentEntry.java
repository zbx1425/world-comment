package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.network.ThumbImage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class CommentEntry {

    public static int REGION_SHIFT = 2;
    public static final int MESSAGE_MAX_LENGTH = 256;

    public long id;
    public long timestamp;
    public ResourceLocation level;
    public ChunkPos region;
    public BlockPos location;
    public UUID initiator;
    public String initiatorName;
    public int messageType;
    public String message;
    public ThumbImage image;

    public boolean deleted;
    public boolean uplinkSent;
    public int like;

    public long fileOffset;

    public CommentEntry(Player initiator, boolean isAnonymous, int messageType, String message) {
        id = ServerWorldData.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        level = initiator #if MC_VERSION >= "12000" .level() #else .level #endif .dimension().location();
        this.initiator = initiator.getGameProfile().getId();
        if (isAnonymous) {
            initiatorName = "";
        } else {
            initiatorName = initiator.getGameProfile().getName();
        }
        this.messageType = messageType;
        this.message = message;
        deleted = false;
    }

    public CommentEntry(ResourceLocation level, FriendlyByteBuf src, boolean fromFile) {
        fileOffset = src.readerIndex();

        deleted = src.readBoolean();
        uplinkSent = src.readBoolean();
        src.skipBytes(2);
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
        image = new ThumbImage(src.readUtf(), src.readUtf());

        if (fromFile) src.skipBytes(16 - (src.readerIndex() % 16));
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
        this.uplinkSent = other.uplinkSent;
        this.like = other.like;
    }

    public void writeBuffer(FriendlyByteBuf dst, boolean toFile) {
        dst.writeBoolean(deleted);
        dst.writeBoolean(uplinkSent);
        dst.writeZero(2);
        dst.writeInt(like);
        dst.writeBytes("====ZBX=".getBytes(StandardCharsets.UTF_8));

        dst.writeLong(id);
        dst.writeLong(timestamp);
        dst.writeBlockPos(location);
        dst.writeUUID(initiator);
        dst.writeUtf(initiatorName);
        dst.writeInt(messageType);
        dst.writeUtf(message);
        dst.writeUtf(image.url);
        dst.writeUtf(image.thumbUrl);

        if (toFile) dst.writeZero(16 - (dst.writerIndex() % 16));
    }

    public void writeFileStream(FileOutputStream oStream) throws IOException {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(512));
        writeBuffer(buf, true);
        fileOffset = oStream.getChannel().position();
        oStream.write(buf.array(), 0, buf.writerIndex());
    }

    public void updateInFile(RandomAccessFile oFile) throws IOException {
        oFile.seek(fileOffset);
        oFile.writeBoolean(deleted);
        oFile.writeBoolean(uplinkSent);
        oFile.write(new byte[2]);
        oFile.writeInt(like);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("timestamp", timestamp);
        json.addProperty("level", level.toString());
        JsonArray blockPosArr = new JsonArray();
        blockPosArr.add(location.getX());
        blockPosArr.add(location.getY());
        blockPosArr.add(location.getZ());
        json.add("location", blockPosArr);
        json.addProperty("initiator", initiator.toString());
        json.addProperty("initiatorName", initiatorName);
        json.addProperty("messageType", messageType);
        json.addProperty("message", message);
        json.add("image", image.toJson());
        json.addProperty("deleted", deleted);
        json.addProperty("like", like);
        return json;
    }

    public ByteBuf toBinaryBuffer() {
        FriendlyByteBuf dest = new FriendlyByteBuf(Unpooled.buffer(512));
        dest.writeResourceLocation(level);
        writeBuffer(dest, false);
        return dest;
    }

    public static CommentEntry fromBinaryBuffer(ByteBuf buf) {
        FriendlyByteBuf src = new FriendlyByteBuf(buf);
        ResourceLocation level = src.readResourceLocation();
        return new CommentEntry(level, src, false);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
