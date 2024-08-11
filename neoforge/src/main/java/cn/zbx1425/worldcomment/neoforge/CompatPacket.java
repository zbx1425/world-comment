package cn.zbx1425.worldcomment.neoforge;

#if MC_VERSION >= "12100"
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class CompatPacket {

    public final ResourceLocation id;

    public CompatPacket(ResourceLocation id) {
        this.id = id;
        this.TYPE = new CustomPacketPayload.Type<>(id);
    }

    public class Payload implements CustomPacketPayload {

        public final FriendlyByteBuf buffer;

        public Payload(FriendlyByteBuf buffer) {
            this.buffer = buffer;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public final CustomPacketPayload.Type<Payload> TYPE;

    public final StreamCodec<ByteBuf, Payload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf dest, Payload src) {
            src.buffer.readerIndex(0);
            dest.writeInt(src.buffer.readableBytes());
            dest.writeBytes(src.buffer);
        }

        @Override
        public Payload decode(ByteBuf src) {
            final int length = src.readInt();
            FriendlyByteBuf result = new FriendlyByteBuf(src.readBytes(length));
            result.readerIndex(0);
            return new Payload(result);
        }
    };
}

#else
public class CompatPacket { }

#endif