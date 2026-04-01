package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.upload.CommentAffinityInfo;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.S3PreSignedUploader;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketPreSignRequestC2S {

    public static final Identifier IDENTIFIER = Main.id("presign_request");

    public static class ClientLogics {

        public static void send(long jobId, CommentAffinityInfo comment, S3PreSignedUploader uploader) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeLong(jobId);
            buffer.writeUUID(uploader.instanceId);
            buffer.writeUtf(comment.initiatorName);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        long jobId = buffer.readLong();
        UUID instanceId = buffer.readUUID();
        String initiatorName = buffer.readUtf();
        CommentAffinityInfo comment = new CommentAffinityInfo(initiator.getGameProfile().id(), initiatorName);

        ImageUploader uploader = Main.SERVER_CONFIG.imageUploaders.value.stream()
                .filter(i -> i.instanceId.equals(instanceId)).findFirst().orElse(null);
        if (!(uploader instanceof S3PreSignedUploader preSignUploader)) {
            Main.LOGGER.warn("Received invalid presign request from {}, instanceId: {}",
                    initiator.getGameProfile().name(), instanceId);
            PacketPreSignResponseS2C.sendException(initiator, jobId, new Exception("Invalid uploader instanceId"));
            return;
        }
        try {
            PacketPreSignResponseS2C.send(initiator, jobId, preSignUploader.performPreSign(comment));
        } catch (Exception ex) {
            PacketPreSignResponseS2C.sendException(initiator, jobId, ex);
        }
    }
}
