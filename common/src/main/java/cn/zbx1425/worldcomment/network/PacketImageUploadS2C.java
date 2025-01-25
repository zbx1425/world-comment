package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketImageUploadS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("image_upload");

    public static void send(ServerPlayer target, long jobId, ThumbImage image) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(jobId);
        buffer.writeBoolean(true);
        buffer.writeUtf(image.url);
        buffer.writeUtf(image.thumbUrl);
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static void sendException(ServerPlayer target, long jobId, Exception ex) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(jobId);
        buffer.writeBoolean(false);
        buffer.writeUtf(ex.toString());
        ServerPlatform.sendPacketToPlayer(target, IDENTIFIER, buffer);
    }

    public static class ClientLogics {
        public static void handle(FriendlyByteBuf buffer) {
            long jobId = buffer.readLong();
            boolean success = buffer.readBoolean();
            if (!success) {
                String error = buffer.readUtf();
                LocalStorageUploader.completeUploadExceptionally(jobId, new Exception(error));
                return;
            }
            String url = buffer.readUtf();
            String thumbUrl = buffer.readUtf();
            ThumbImage image = new ThumbImage(url, thumbUrl);
            LocalStorageUploader.completeUpload(jobId, image);
        }
    }
} 