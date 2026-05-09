package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.item.PlaceableCommentItem;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PacketRequestPlacementC2S {

    public static final Identifier IDENTIFIER = Main.id("request_placement");

    public static class ClientLogics {

        public static void sendBeginPlacement(long uploadJobId) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeInt(1);
            buffer.writeLong(uploadJobId);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }

        public static void sendEndPlacement() {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeInt(2);
            buffer.writeLong(0);
            ClientPlatform.sendPacketToServer(IDENTIFIER, buffer);
        }
    }

    public static void handle(MinecraftServer server, ServerPlayer initiator, FriendlyByteBuf buffer) {
        boolean isBeginningPlacement = buffer.readInt() == 1;
        long uploadJobId = buffer.readLong();

        ItemStack previousStack = initiator.getMainHandItem();
        ItemStack newStack;
        if (isBeginningPlacement) {
            newStack = PlaceableCommentItem.createStack(uploadJobId, previousStack);
        } else {
            newStack = PlaceableCommentItem.unboxStack(previousStack);
        }
        initiator.setItemSlot(EquipmentSlot.MAINHAND, newStack);
    }
}
