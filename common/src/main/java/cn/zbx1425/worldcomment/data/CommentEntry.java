package cn.zbx1425.worldcomment.data;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class CommentEntry {

    public static int REGION_SHIFT = 2;

    public long id;
    public long timestamp;
    public ResourceLocation level;
    public ChunkPos region;
    public BlockPos location;
    public UUID initiator;
    public String initiatorName;
    public int messageType;
    public String message;
    public boolean deleted;
    public String imageUrl;

    public long fileOffset;

    public CommentEntry(Player initiator, BlockPos placedAt, int messageType, String message, String imageUrl) {
        id = Database.SNOWFLAKE.nextId();
        timestamp = System.currentTimeMillis();
        level = initiator.level().dimension().location();
        location = placedAt;
        region = new ChunkPos(location.getX() >> REGION_SHIFT, location.getZ() >> REGION_SHIFT);
        this.initiator = initiator.getGameProfile().getId();
        initiatorName = initiator.getGameProfile().getName();
        this.messageType = messageType;
        this.message = message;
        deleted = false;
        this.imageUrl = imageUrl;
    }

    public CommentEntry(ResourceLocation level, FriendlyByteBuf src) {
        fileOffset = src.readerIndex();
        id = src.readLong();
        timestamp = src.readLong();
        this.level = level;
        location = src.readBlockPos();
        region = new ChunkPos(location.getX() >> REGION_SHIFT, location.getZ() >> REGION_SHIFT);
        initiator = src.readUUID();
        initiatorName = src.readUtf();
        messageType = src.readInt();
        message = src.readUtf();
        deleted = src.readBoolean();
        imageUrl = src.readUtf();
        src.skipBytes(16 - (src.readerIndex() % 16));
    }

    private static UUID uuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    public void writeBuffer(FriendlyByteBuf dst) {
        dst.writeLong(id);
        dst.writeLong(timestamp);
        dst.writeBlockPos(location);
        dst.writeUUID(initiator);
        dst.writeUtf(initiatorName);
        dst.writeInt(messageType);
        dst.writeUtf(message);
        dst.writeBoolean(deleted);
        dst.writeUtf(imageUrl);
        dst.writeZero(16 - (dst.writerIndex() % 16));
    }

    public void writeFileStream(FileOutputStream oStream) throws IOException {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(256));
        writeBuffer(buf);
        fileOffset = oStream.getChannel().position();
        oStream.write(buf.array(), 0, buf.writerIndex());
    }

}
