package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.SubmitDispatcher;
#if MC_VERSION >= "12000" import net.minecraft.core.registries.Registries; #endif
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
#if MC_VERSION >= "12100" import net.minecraft.core.component.DataComponents; #endif
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
#if MC_VERSION >= "12100" import net.minecraft.world.item.component.CustomData; #endif
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class CommentToolItem extends Item implements GroupedItem {

    private static boolean visibilityPreference = true;
    public static float invisibleTimeRemaining = 0f;

    public CommentToolItem() {
        super(GroupedItem.createProperties(properties ->
                properties.stacksTo(1)
        ));
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

    @Override
    public ResourceKey<CreativeModeTab> getTab() {
        #if MC_VERSION >= "12000"
            return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Main.vanillaId("tools_and_utilities"));
        #else
            return CreativeModeTab.TAB_MISC;
        #endif
    }

    public static class Client {

        public static final int COMMENT_HIDE_TICKS = 60 * 4 * 20;

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
            Long jobId = getUploadJobId(item);
            if (jobId != null) {
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
                        setUploadJobId(item, null);
                        return true;
                    } else {
                        player.displayClientMessage(
                                Component.translatable("gui.worldcomment.send_insufficient_clearance"), false);
                    }
                }
            } else {
                if (visibilityPreference) {
                    visibilityPreference = false;
                    if (COMMENT_HIDE_TICKS - (invisibleTimeRemaining % COMMENT_HIDE_TICKS) < 60) {
                        invisibleTimeRemaining =
                                Math.min(Math.round((invisibleTimeRemaining + COMMENT_HIDE_TICKS) / COMMENT_HIDE_TICKS), 4) * COMMENT_HIDE_TICKS;
                    } else {
                        invisibleTimeRemaining = COMMENT_HIDE_TICKS;
                    }
                } else {
                    visibilityPreference = true;
                }
            }
            return false;
        }
    }

    public static Long getUploadJobId(ItemStack item) {
#if MC_VERSION >= "12100"
        CustomData customData = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.contains("uploadJobId")) {
            return customData.copyTag().getLong("uploadJobId");
        } else {
            return null;
        }
#else
        if (item.getOrCreateTag().contains("uploadJobId", Tag.TAG_LONG)) {
            return item.getOrCreateTag().getLong("uploadJobId");
        } else {
            return null;
        }
#endif
    }

    public static void setUploadJobId(ItemStack item, Long jobId) {
#if MC_VERSION >= "12100"
        if (jobId == null) {
            item.remove(DataComponents.CUSTOM_DATA);
        } else {
            CompoundTag tag = new CompoundTag();
            tag.putLong("uploadJobId", jobId);
            item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
#else
        if (jobId == null) {
            item.getOrCreateTag().remove("uploadJobId");
        } else {
            item.getOrCreateTag().putLong("uploadJobId", jobId);
        }
#endif
    }

    public static void updateInvisibilityTimer(float deltaTicks) {
        if (invisibleTimeRemaining > 0) {
            invisibleTimeRemaining -= deltaTicks;
            if (invisibleTimeRemaining <= 0) {
                visibilityPreference = true;
            }
        }
    }

    public static boolean getVisibilityPreference() {
        return visibilityPreference;
    }
}
