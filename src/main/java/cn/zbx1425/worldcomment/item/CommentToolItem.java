package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
#if MC_VERSION >= "12000"
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.mixin.KeyMappingAccessor;
import cn.zbx1425.worldcomment.util.FrameTask;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.registries.Registries; #endif
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
#if MC_VERSION < "12108" import net.minecraft.world.InteractionResultHolder; #endif
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.function.Function;

public class CommentToolItem extends Item implements GroupedItem {

    public CommentToolItem() {
        super(GroupedItem.createProperties(properties ->
                properties.stacksTo(1)
        , Main.id("comment_tool"), CommentToolItem::getTabImpl));
    }

    @Override
    public #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTab() {
        return getTabImpl();
    }

    public static #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTabImpl() {
        #if MC_VERSION >= "12000"
            return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Main.vanillaId("tools_and_utilities"));
        #else
            return CreativeModeTab.TAB_MISC;
        #endif
    }

    public static class Client {

        public static ItemStack getHolding() {
            Player player = Minecraft.getInstance().player;
            if (player == null) return null;
            ItemStack mainHandStack = player.getMainHandItem();
            if (mainHandStack.is(Main.ITEM_COMMENT_TOOL.get())) return mainHandStack;
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.is(Main.ITEM_COMMENT_TOOL.get())) return offHandStack;
            return null;
        }

        private static final int[] MODIFIER_SYMS = new int[] {
            GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT,
            GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL
        };
        private static final Function<InputConstants.Key, Boolean> HOTKEY_IS_MODIFIER_SUPPLIER = Util.memoize(_ ->
            Arrays.stream(MODIFIER_SYMS).anyMatch(it -> ((KeyMappingAccessor)MainClient.KEY_SEND_COMMENT_MODIFIER.get()).getKey().getValue() == it)
        );
        private static final Function<InputConstants.Key, Component> HOTKEY_DESCRIPTION_SUPPLIER = Util.memoize(_ -> {
            if (HOTKEY_IS_MODIFIER_SUPPLIER.apply(((KeyMappingAccessor)MainClient.KEY_SEND_COMMENT_MODIFIER.get()).getKey())) {
                return MainClient.KEY_SEND_COMMENT_MODIFIER.get().getTranslatedKeyMessage().copy()
                    .append(" + ")
                    .append(Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage());
            } else {
                return MainClient.KEY_SEND_COMMENT_MODIFIER.get().getTranslatedKeyMessage();
            }
        });
        public static boolean getSendHotkeyIsModifier() {
            return HOTKEY_IS_MODIFIER_SUPPLIER.apply(((KeyMappingAccessor)MainClient.KEY_SEND_COMMENT_MODIFIER.get()).getKey());
        }
        public static Component getSendHotkeyDescription() {
            return HOTKEY_DESCRIPTION_SUPPLIER.apply(((KeyMappingAccessor)MainClient.KEY_SEND_COMMENT_MODIFIER.get()).getKey());
        }

        public static CommentEntry getUsageHelpMessage() {
            String usageHelpContent = Component.translatable("gui.worldcomment.instruction.send_header").getString() + "\n"
                + Component.translatable("gui.worldcomment.instruction.send_content",
                getSendHotkeyDescription().copy()
            ).getString() + "\n\n"
                + Component.translatable(
                MainClient.CLIENT_CONFIG.perServerPreference.commentVisibilityPreference
                    ? "gui.worldcomment.instruction.hide_header"
                    : "gui.worldcomment.instruction.hide_header_cta"
            ).getString() + "\n"
                + Component.translatable("gui.worldcomment.instruction.hide_content").getString() + "\n\n"
                + Component.translatable("gui.worldcomment.instruction.list_header").getString() + "\n"
                + Component.translatable("gui.worldcomment.instruction.list_content").getString();
            return CommentEntry.createSystemMessage(
                EmojiRegistry.HIGH_EMOJI_BASE_ID,
                usageHelpContent,
                Component.translatable("gui.worldcomment.instruction.title").getString()
            );
        }

        public static boolean triggerCommentSend(boolean withPlacingDown) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return false;
            if (minecraft.screen != null && !(minecraft.screen instanceof ChatScreen)) return false;
            if (PlaceableCommentItem.Client.getHolding() != null) return false;
            if (Screenshot.isGrabbing) return false;
            minecraft.player.playSound(shutterSoundEvent);

            Screenshot.grabScreenshot(imageBytes -> {
                Minecraft.getInstance().setScreen(new CommentToolScreen(imageBytes, withPlacingDown));
            });
            return true;
        }

        private static final SoundEvent shutterSoundEvent = #if MC_VERSION >= "11903" SoundEvent.createFixedRangeEvent #else new SoundEvent #endif (
            Main.id("shutter"), 16
        );
    }
}
