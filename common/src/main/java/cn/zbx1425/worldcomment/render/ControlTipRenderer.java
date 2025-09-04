package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.IGuiCommon;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class ControlTipRenderer implements IGuiCommon {

    public static final ControlTip TIP_CREATE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.create"), TipLogo.KEYBOARD,
            Minecraft.getInstance().options.keyScreenshot, false, true
    );
    public static final ControlTip TIP_PLACE_COMMENT = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.place_comment"), TipLogo.RIGHT_MOUSE_BUTTON,
            null, true, true
    );
    public static final ControlTip TIP_TOGGLE_SHOW = new ControlTip(
            () -> Component.translatable("gui.worldcomment.control_tip.toggle_show",
                    ClientRayPicking.nearbyCommentsCount), TipLogo.SN_LOGO,
            null, false
    );
    public static final ControlTip TIP_TOGGLE_HIDE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.toggle_hide"), TipLogo.RIGHT_MOUSE_BUTTON,
            null, false
    );
    public static final ControlTip TIP_VIEW_MANAGE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.view_manage"), TipLogo.KEYBOARD,
            Minecraft.getInstance().options.keyTogglePerspective, false
    );
    public static final ControlTip TIP_DETAIL = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.detail"), TipLogo.KEYBOARD,
            Minecraft.getInstance().options.keyPlayerList, false
    );
    public static final ControlTip TIP_SCROLL = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.scroll"), TipLogo.SCROLL_WHEEL,
            null, false
    );

    public static final List<ControlTip> TIPS =
            List.of(TIP_PLACE_COMMENT, TIP_TOGGLE_SHOW, TIP_TOGGLE_HIDE, TIP_CREATE,
                    TIP_VIEW_MANAGE, TIP_SCROLL, TIP_DETAIL);

    public static void render(ISnGuiGraphics guiGraphics) {
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
        TIP_TOGGLE_SHOW.visible = !MainClient.CLIENT_CONFIG.commentVisibilityPreference;
        ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
        if (item != null) {
            if (CommentToolItem.getUploadJobId(item) != null) {
                TIP_PLACE_COMMENT.visible = true;
            } else {
                TIP_CREATE.visible = true;
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

        public final TipLogo logo;
        public final KeyMapping key;
        public final boolean critical;
        public final boolean atCursor;
        public final Supplier<Component> text;

        public boolean visible = false;

        public ControlTip(Component text, TipLogo logo, KeyMapping key, boolean critical) {
            this.text = () -> text;
            this.logo = logo;
            this.key = key;
            this.critical = critical;
            this.atCursor = false;
        }

        public ControlTip(Component text, TipLogo logo, KeyMapping key, boolean critical, boolean atCursor) {
            this.text = () -> text;
            this.logo = logo;
            this.key = key;
            this.critical = critical;
            this.atCursor = atCursor;
        }

        public ControlTip(Supplier<Component> text, TipLogo logo, KeyMapping key, boolean critical) {
            this.text = text;
            this.logo = logo;
            this.key = key;
            this.critical = critical;
            this.atCursor = false;
        }

        public void render(ISnGuiGraphics guiGraphics, int x, int y) {
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
                    logo.u, logo.v, logo.size, logo.size, 256, 256);
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

    public enum TipLogo {
        RIGHT_MOUSE_BUTTON(176, 0, 20),
        SCROLL_WHEEL(196, 0, 20),
        KEYBOARD(216, 0, 20),
        SN_LOGO(176, 96, 32);

        public final int u;
        public final int v;
        public final int size;

        TipLogo(int u, int v, int size) {
            this.u = u;
            this.v = v;
            this.size = size;
        }
    }
}
