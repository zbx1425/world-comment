package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.SubmitDispatcher;
import cn.zbx1425.worldcomment.network.PacketRequestPlacementC2S;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PlaceableCommentItem extends Item implements GroupedItem {

    public PlaceableCommentItem() {
        super(new Properties().stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, Main.id("placeable_comment"))));
    }

    public static Either<Integer, ItemStack> unboxStack(ItemStack self) {
        if (!self.is(Main.ITEM_PLACEABLE_COMMENT.get())) {
            return Either.right(self);
        } else {
            MetaComponent meta = self.get(Main.DATA_COMPONENT_TYPE_PLACEABLE_COMMENT_META.get());
            if (meta == null) return Either.left(-1);
            if (meta.encapsulatedStack.isEmpty()) return Either.left(meta.swappedSlotId);
            return unboxStack(meta.encapsulatedStack());
        }
    }

    public static ItemStack createStack(long clientJobId, int swappedSlotId) {
        ItemStack result = new ItemStack(Main.ITEM_PLACEABLE_COMMENT.get());
        MetaComponent meta = new MetaComponent(clientJobId, swappedSlotId, ItemStack.EMPTY);
        result.set(Main.DATA_COMPONENT_TYPE_PLACEABLE_COMMENT_META.get(), meta);
        return result;
    }

    public static ItemStack createStack(long clientJobId, ItemStack encapsulatedStack) {
        ItemStack result = new ItemStack(Main.ITEM_PLACEABLE_COMMENT.get());
        MetaComponent meta = new MetaComponent(clientJobId, -1, encapsulatedStack);
        result.set(Main.DATA_COMPONENT_TYPE_PLACEABLE_COMMENT_META.get(), meta);
        return result;
    }

    @Override
    public @NotNull #if MC_VERSION < "12108" InteractionResultHolder<ItemStack> #else InteractionResult #endif use(Level level, Player player, InteractionHand usedHand) {
        ItemStack item = player.getItemInHand(usedHand);
        if (!level.isClientSide()) return #if MC_VERSION < "12108" InteractionResultHolder.pass(item) #else InteractionResult.PASS #endif;
        if (usedHand != InteractionHand.MAIN_HAND
            || !item.is(Main.ITEM_PLACEABLE_COMMENT.get())) return #if MC_VERSION < "12108" InteractionResultHolder.fail(item) #else InteractionResult.FAIL #endif;

        if (Client.placeUploadJob(level, player, item)) {
            return #if MC_VERSION < "12108" InteractionResultHolder.success(item) #else InteractionResult.SUCCESS #endif;
        } else {
            return #if MC_VERSION < "12108" InteractionResultHolder.fail(item) #else InteractionResult.FAIL #endif;
        }
    }

    public static class Client {

        public static boolean placeUploadJob(Level level, Player player, ItemStack item) {
            MetaComponent meta = item.get(Main.DATA_COMPONENT_TYPE_PLACEABLE_COMMENT_META.get());
            if (meta != null) {
                HitResult hitResult = Minecraft.getInstance().hitResult;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    BlockPos facePos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());

                    // Snap down
                    BlockPos groundPos = facePos.below();
                    boolean hasSupport = false;
                    for (int i = 0; i <= 5; i++) {
                        if (level.getBlockState(groundPos).isSolid()) {
                            facePos = groundPos.above();
                            hasSupport = true;
                            break;
                        }
                        groundPos = groundPos.below();
                    }

                    if (!hasSupport) {
                        player.sendSystemMessage(
                            Component.translatable("gui.worldcomment.send_in_air"));
                    }
                    SubmitDispatcher.placeJobAt(meta.clientJobId(), facePos);
                    PacketRequestPlacementC2S.ClientLogics.sendEndPlacement();
                    return true;
                }
            }
            return false;
        }

        public static ItemStack getHolding() {
            Player player = Minecraft.getInstance().player;
            if (player == null) return null;
            ItemStack mainHandStack = player.getMainHandItem();
            if (mainHandStack.is(Main.ITEM_PLACEABLE_COMMENT.get())) return mainHandStack;
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.is(Main.ITEM_PLACEABLE_COMMENT.get())) return offHandStack;
            return null;
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        MetaComponent meta = itemStack.get(Main.DATA_COMPONENT_TYPE_PLACEABLE_COMMENT_META.get());
        if (meta != null && !meta.encapsulatedStack.isEmpty()) {
            builder.accept(Component.translatable("gui.worldcomment.placeable_comment.encapsulation", meta.encapsulatedStack.getDisplayName())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));
        }
    }

    @Override
    public ResourceKey<CreativeModeTab> getTab() {
        return null;
    }

    public record MetaComponent(long clientJobId, int swappedSlotId, ItemStack encapsulatedStack) {
        public static final Codec<MetaComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("client_job_id").forGetter(MetaComponent::clientJobId),
                Codec.INT.fieldOf("swapped_slot_id").forGetter(MetaComponent::swappedSlotId),
                ItemStack.OPTIONAL_CODEC.fieldOf("encapsulated_stack").forGetter(MetaComponent::encapsulatedStack)
            ).apply(instance, MetaComponent::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, MetaComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, MetaComponent::clientJobId,
            ByteBufCodecs.INT, MetaComponent::swappedSlotId,
            ItemStack.OPTIONAL_STREAM_CODEC, MetaComponent::encapsulatedStack,
            MetaComponent::new
        );
    }
}
