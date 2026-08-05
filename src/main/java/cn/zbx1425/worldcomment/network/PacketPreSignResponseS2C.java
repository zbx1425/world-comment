package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.platform.ServerPlatform;
import cn.zbx1425.worldcomment.data.network.upload.S3PreSignedUploader;
import cn.zbx1425.worldcomment.data.network.upload.ImageFilePurpose;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PacketPreSignResponseS2C {

    public static final Identifier IDENTIFIER = Main.id("presign_response");

    public static void send(ServerPlayer target, long jobId, S3PreSignedUploader.PreSignResponse preSignResponse) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeLong(jobId);
        buffer.writeBoolean(true);
        buffer.writeInt(preSignResponse.slots().size());
        for (S3PreSignedUploader.PreSignedSlot slot : preSignResponse.slots()) {
            buffer.writeEnum(slot.purpose());
            buffer.writeUtf(slot.uploadUrl());
            buffer.writeUtf(slot.accessUrl());
        }
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
            int slotCount = buffer.readInt();
            List<S3PreSignedUploader.PreSignedSlot> slots = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                ImageFilePurpose purpose = buffer.readEnum(ImageFilePurpose.class);
                String uploadUrl = buffer.readUtf();
                String accessUrl = buffer.readUtf();
                slots.add(new S3PreSignedUploader.PreSignedSlot(purpose, uploadUrl, accessUrl));
            }
            S3PreSignedUploader.completePreSign(jobId, new S3PreSignedUploader.PreSignResponse(slots));
        }
    }
}
