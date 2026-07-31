package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.network.CommentImage;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PacketImageUploadS2C {

    public static final Identifier IDENTIFIER = Main.id("image_upload");

    public static void send(ServerPlayer target, long jobId, CommentImage image) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(jobId);
        buffer.writeBoolean(true);
        image.writePacket(buffer);
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
            CommentImage image = CommentImage.readPacket(buffer);
            LocalStorageUploader.completeUpload(jobId, image);
        }
    }
}
