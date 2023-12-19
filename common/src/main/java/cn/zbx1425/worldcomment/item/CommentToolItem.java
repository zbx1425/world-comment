package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.data.network.SubmitDispatcher;
#if MC_VERSION >= "12000" import cn.zbx1425.worldcomment.mixin.CreativeModeTabsAccessor; #endif
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class CommentToolItem extends GroupedItem {

    public CommentToolItem() {
        super(
            () -> #if MC_VERSION >= "12000" CreativeModeTabsAccessor.getTOOLS_AND_UTILITIES() #else CreativeModeTab.TAB_MISC #endif,
            properties -> properties.stacksTo(1)
        );
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack item = player.getItemInHand(usedHand);
        if (!level.isClientSide) return InteractionResultHolder.pass(item);
        if (!item.is(Main.ITEM_COMMENT_TOOL.get())) return InteractionResultHolder.fail(item);

        if (Client.placeUploadJob(level, player, item)) {
            return InteractionResultHolder.success(item);
        } else {
            return InteractionResultHolder.fail(item);
        }
    }

    public static class Client {

        public static ItemStack getHoldingCommentTool() {
            Player player = Minecraft.getInstance().player;
            if (player == null) return null;
            ItemStack mainHandStack = player.getMainHandItem();
            if (mainHandStack.is(Main.ITEM_COMMENT_TOOL.get())) return mainHandStack;
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.is(Main.ITEM_COMMENT_TOOL.get())) return offHandStack;
            return null;
        }

        public static boolean placeUploadJob(Level level, Player player, ItemStack item) {
            if (item.getOrCreateTag().contains("uploadJobId", Tag.TAG_LONG)) {
                long jobId = item.getOrCreateTag().getLong("uploadJobId");
                HitResult hitResult = Minecraft.getInstance().hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    BlockPos facePos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                    boolean hasClearance = true;
                    for (int y = 0; y < 3; y++) {
                        if (level.getBlockState(facePos.offset(0, y, 0)) #if MC_VERSION < "12000" .getMaterial() #endif .isSolid()) {
                            hasClearance = false;
                            break;
                        }
                    }
                    if (hasClearance) {
                        SubmitDispatcher.placeJobAt(jobId, facePos);
                        item.getOrCreateTag().remove("uploadJobId");
                        return true;
                    } else {
                        player.displayClientMessage(
                                Component.translatable("gui.worldcomment.send_insufficient_clearance"), false);
                    }
                }
            } else {
                MainClient.CLIENT_CONFIG.isCommentVisible = !MainClient.CLIENT_CONFIG.isCommentVisible;
            }
            return false;
        }
    }
}
