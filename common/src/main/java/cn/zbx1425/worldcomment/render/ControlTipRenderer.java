package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.IGuiCommon;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class ControlTipRenderer implements IGuiCommon {

    public static final ControlTip TIP_CREATE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.create"), 2,
            Minecraft.getInstance().options.keyScreenshot, false, true
    );
    public static final ControlTip TIP_PLACE_COMMENT = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.place_comment"), 0,
            null, true, true
    );
    public static final ControlTip TIP_TOGGLE_SHOW = new ControlTip(
            () -> Component.translatable("gui.worldcomment.control_tip.toggle_show",
                    String.format("%02d:%02d",
                            (int)Math.floor((CommentToolItem.invisibleTimeRemaining / 20) / 60),
                            (int)Math.floor((CommentToolItem.invisibleTimeRemaining / 20) % 60)
                    )), 0,
            null, true
    );
    public static final ControlTip TIP_TOGGLE_HIDE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.toggle_hide"), 0,
            null, false
    );
    public static final ControlTip TIP_VIEW_MANAGE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.view_manage"), 2,
            Minecraft.getInstance().options.keyTogglePerspective, false
    );
    public static final ControlTip TIP_DETAIL = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.detail"), 2,
            Minecraft.getInstance().options.keyPlayerList, false
    );
    public static final ControlTip TIP_SCROLL = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.scroll"), 1,
            null, false
    );

    public static final List<ControlTip> TIPS =
            List.of(TIP_PLACE_COMMENT, TIP_TOGGLE_SHOW, TIP_TOGGLE_HIDE, TIP_CREATE,
                    TIP_VIEW_MANAGE, TIP_SCROLL, TIP_DETAIL);

    public static void render(GuiGraphics guiGraphics) {
        update();
        int yOffset = 10;
        for (ControlTip tip : TIPS) {
            if (!tip.visible) continue;
            tip.render(guiGraphics, 10, yOffset);
            if (!tip.atCursor) yOffset += 20 + 2;
        }
    }

    public static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ControlTip tip : TIPS) tip.visible = false;
        if (minecraft.player == null) {
            return;
        }
        if (Screenshot.isGrabbing) return;
        ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
        if (item != null) {
            if (CommentToolItem.getUploadJobId(item) != null) {
                TIP_PLACE_COMMENT.visible = true;
            } else {
                TIP_CREATE.visible = true;
                if (CommentToolItem.getVisibilityPreference()) {
                    TIP_TOGGLE_HIDE.visible = true;
                } else {
                    TIP_TOGGLE_SHOW.visible = true;
                }
            }
            TIP_VIEW_MANAGE.visible = true;
        }
        if (!ClientRayPicking.pickedComments.isEmpty()) {
            if (ClientRayPicking.pickedComments.size() > 1) {
                TIP_SCROLL.visible = true;
            }
            TIP_DETAIL.visible = true;
        }
    }

    public static class ControlTip {

        public final int imgIndex;
        public final KeyMapping key;
        public final boolean critical;
        public final boolean atCursor;
        public final Supplier<Component> text;

        public boolean visible = false;

        public ControlTip(Component text, int imgIndex, KeyMapping key, boolean critical) {
            this.text = () -> text;
            this.imgIndex = imgIndex;
            this.key = key;
            this.critical = critical;
            this.atCursor = false;
        }

        public ControlTip(Component text, int imgIndex, KeyMapping key, boolean critical, boolean atCursor) {
            this.text = () -> text;
            this.imgIndex = imgIndex;
            this.key = key;
            this.critical = critical;
            this.atCursor = atCursor;
        }

        public ControlTip(Supplier<Component> text, int imgIndex, KeyMapping key, boolean critical) {
            this.text = text;
            this.imgIndex = imgIndex;
            this.key = key;
            this.critical = critical;
            this.atCursor = false;
        }

        public void render(GuiGraphics guiGraphics, int x, int y) {
            Font font = Minecraft.getInstance().font;
            int innerWidth = 20 + 4 + font.width(text.get());
            if (atCursor) {
                x = guiGraphics.guiWidth() / 2 - innerWidth / 2;
                y = guiGraphics.guiHeight() / 2 - 20 - 4;
            }
            if (critical) {
                long currentTime = System.currentTimeMillis();
                if (currentTime % 400 < 200) {
                    guiGraphics.fill(x + 1, y + 1, x + innerWidth + 4 + 1, y + 20 + 1, 0xFF444444);
                    guiGraphics.fill(x, y, x + innerWidth + 4, y + 20, 0xFFDDDD66);
                }
            }
            guiGraphics.blit(ATLAS_LOCATION, x, y, 20, 20,
                    176 + imgIndex * 20, 0, 20, 20, 256, 256);
            if (key != null) {
                Component keyMessage = key.getTranslatedKeyMessage();
                int keyMessageWidth = font.width(keyMessage);
                if (keyMessageWidth < 30) {
                    guiGraphics.drawCenteredString(font, keyMessage, x + 10, y + 10 - 4, 0xFFFFECB3);
                } else {
                    guiGraphics.enableScissor(x, y, x + 20, y + 20);
                    int xOffset = (int)((System.currentTimeMillis() / 50) % (keyMessageWidth + 40));
                    guiGraphics.drawString(font, keyMessage, x - xOffset + 20, y + 10 - 4, 0xFFFFECB3);
                    guiGraphics.disableScissor();
                }
            }
            guiGraphics.drawString(font, text.get(), x + 20 + 4, y + 10 - 4, 0xFFFFFFFF, true);
        }
    }
}
