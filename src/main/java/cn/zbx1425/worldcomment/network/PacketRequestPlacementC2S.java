package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.platform.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.item.PlaceableCommentItem;
import com.mojang.datafixers.util.Either;
import io.netty.buffer.Unpooled;
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
            // If main hand is empty, then no need for swapping
            int freeSlot = initiator.getMainHandItem().isEmpty() ? -1 : initiator.getInventory().getFreeSlot();
            if (freeSlot >= 0) {
                // Swap main hand item into a free slot
                initiator.getInventory().setItem(freeSlot, previousStack);
                newStack = PlaceableCommentItem.createStack(uploadJobId, freeSlot);
            } else {
                // Inventory is full, encapsulate the previous item
                newStack = PlaceableCommentItem.createStack(uploadJobId, previousStack);
            }
        } else {
            Either<Integer, ItemStack> swapInstructionOrEncapsulation = PlaceableCommentItem.unboxStack(previousStack);
            if (swapInstructionOrEncapsulation.left().isPresent()) {
                // Swap the item previously put into free slot back
                // -1: No need to swap
                if (swapInstructionOrEncapsulation.left().get() >= 0) {
                    newStack = initiator.getInventory().getItem(swapInstructionOrEncapsulation.left().get());
                    initiator.getInventory().setItem(swapInstructionOrEncapsulation.left().get(), ItemStack.EMPTY);
                } else {
                    newStack = ItemStack.EMPTY;
                }
            } else {
                // Unbox
                newStack = swapInstructionOrEncapsulation.right().orElseThrow();
            }
        }
        initiator.setItemSlot(EquipmentSlot.MAINHAND, newStack);
    }
}
