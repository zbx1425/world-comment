package cn.zbx1425.worldcomment.network;

import cn.zbx1425.worldcomment.ClientPlatform;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PacketDemandToolPresenceC2S {

    public static final Identifier IDENTIFIER = Main.id("demand_tool_presence");

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

        ItemStack targetStack;
        boolean stackIsNewlySpawned;
        int previousMainHandItemIsNowInSlot;
        if (initiator.getMainHandItem().is(Main.ITEM_COMMENT_TOOL.get())) {
            // The player has a tool in the main hand
            targetStack = initiator.getMainHandItem();
            previousMainHandItemIsNowInSlot = -1; // Not moved
            stackIsNewlySpawned = false;
        } else if (initiator.getOffhandItem().is(Main.ITEM_COMMENT_TOOL.get())) {
            // The player has a tool in the offhand, swap it to main hand
            ItemStack tmp = initiator.getItemBySlot(EquipmentSlot.OFFHAND);
            initiator.setItemSlot(EquipmentSlot.OFFHAND, initiator.getItemBySlot(EquipmentSlot.MAINHAND));
            initiator.setItemSlot(EquipmentSlot.MAINHAND, tmp);
            targetStack = initiator.getMainHandItem();
            previousMainHandItemIsNowInSlot = Inventory.SLOT_OFFHAND;
            stackIsNewlySpawned = false;
        } else {
            // Does the player have one elsewhere in the inventory?
            ItemStack existingStack = null;
            int existingStackSlot = 0;
            for (int i = 0; i < initiator.getInventory().getContainerSize(); i++) {
                if (initiator.getInventory().getSlot(i).get().is(Main.ITEM_COMMENT_TOOL.get())) {
                    existingStack = initiator.getInventory().getSlot(i).get();
                    existingStackSlot = i;
                    break;
                }
            }
            if (existingStack != null) {
                // There is one, swap it to main hand
                initiator.getInventory().setItem(existingStackSlot, initiator.getItemBySlot(EquipmentSlot.MAINHAND));
                initiator.setItemSlot(EquipmentSlot.MAINHAND, existingStack);
                targetStack = initiator.getMainHandItem();
                stackIsNewlySpawned = false;
                previousMainHandItemIsNowInSlot = existingStackSlot;
            } else {
                // Try give the player one
                int freeSlot = initiator.getInventory().getFreeSlot();
                if (freeSlot == -1) {
                    // Inventory is full! Drop an unfortunate item?
                    targetStack = null;
                } else {
                    // Swap the main hand item into the free slot, and put the new item into main hand
                    ItemStack newStack = new ItemStack(Holder.direct(Main.ITEM_COMMENT_TOOL.get()));
                    initiator.getInventory().setItem(freeSlot, initiator.getItemBySlot(EquipmentSlot.MAINHAND));
                    initiator.setItemSlot(EquipmentSlot.MAINHAND, newStack);
                    targetStack = initiator.getMainHandItem();
                }
                stackIsNewlySpawned = true;
                previousMainHandItemIsNowInSlot = freeSlot;
            }
        }

        if (targetStack != null) {
            if (isBeginningPlacement) {
                CommentToolItem.setDataForBeginningPlacement(targetStack, uploadJobId,
                    stackIsNewlySpawned, previousMainHandItemIsNowInSlot);
            } else {
                // The comment tool item should be guaranteed to be in main hand for ending placement
                CommentToolItem.PlacementEndResult nextAction = CommentToolItem.setDataForEndingPlacement(targetStack);
                if (nextAction.shouldCommentToolBeRemoved) {
                    initiator.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
                if (nextAction.slotToBeSwappedIntoMainHand >= 0) {
                    ItemStack existingStack = initiator.getInventory().getItem(nextAction.slotToBeSwappedIntoMainHand);
                    initiator.getInventory().setItem(nextAction.slotToBeSwappedIntoMainHand, initiator.getItemBySlot(EquipmentSlot.MAINHAND));
                    initiator.setItemSlot(EquipmentSlot.MAINHAND, existingStack);
                }
            }
        }
    }
}
