package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerPlatform;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.data.network.upload.LocalStorageUploader;
import cn.zbx1425.worldcomment.data.network.upload.S3PreSignedUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PacketPreSignResponseS2C {

    public static final ResourceLocation IDENTIFIER = Main.id("presign_response");

    public static void send(ServerPlayer target, long jobId, S3PreSignedUploader.PreSignResponse preSignResponse) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(jobId);
        buffer.writeBoolean(true);
        buffer.writeUtf(preSignResponse.upload().url);
        buffer.writeUtf(preSignResponse.upload().thumbUrl);
        buffer.writeUtf(preSignResponse.access().url);
        buffer.writeUtf(preSignResponse.access().thumbUrl);
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
                S3PreSignedUploader.completePreSignExceptionally(jobId, new Exception(error));
                return;
            }
            String url = buffer.readUtf();
            String thumbUrl = buffer.readUtf();
            String accessUrl = buffer.readUtf();
            String accessThumbUrl = buffer.readUtf();
            S3PreSignedUploader.PreSignResponse response = new S3PreSignedUploader.PreSignResponse(
                    new ThumbImage(url, thumbUrl),
                    new ThumbImage(accessUrl, accessThumbUrl)
            );
            S3PreSignedUploader.completePreSign(jobId, response);
        }
    }
}
