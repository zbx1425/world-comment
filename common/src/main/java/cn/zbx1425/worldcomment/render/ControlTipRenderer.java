package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.IGuiCommon;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ControlTipRenderer implements IGuiCommon {

    public static final ControlTip TIP_CREATE = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.create"), 2,
            Minecraft.getInstance().options.keyScreenshot, false
    );
    public static final ControlTip TIP_PLACE_COMMENT = new ControlTip(
            Component.translatable("gui.worldcomment.control_tip.place_comment"), 0,
            null, true
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
            List.of(TIP_PLACE_COMMENT, TIP_CREATE, TIP_VIEW_MANAGE, TIP_SCROLL, TIP_DETAIL);

    public static void render(GuiGraphics guiGraphics) {
        update();
        int yOffset = 10;
        for (ControlTip tip : TIPS) {
            if (!tip.visible) continue;
            tip.render(guiGraphics, 10, yOffset);
            yOffset += 20 + 2;
        }
    }

    public static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ControlTip tip : TIPS) tip.visible = false;
        if (minecraft.player == null) {
            return;
        }
        ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
        if (item != null) {
            if (item.getOrCreateTag().contains("uploadJobId", Tag.TAG_LONG)) {
                TIP_PLACE_COMMENT.visible = true;
                return; // De-clutter
            } else {
                TIP_CREATE.visible = true;
            }
            TIP_VIEW_MANAGE.visible = true;
        }
        if (ClientRayPicking.pickedComments.size() > 0) {
            if (ClientRayPicking.pickedComments.size() > 1) {
                TIP_SCROLL.visible = true;
            }
            TIP_DETAIL.visible = true;
        }
    }

    public static class ControlTip {

        public int imgIndex;
        public KeyMapping key;
        public boolean critical;
        public Component text;

        public boolean visible = false;

        public ControlTip(Component text, int imgIndex, KeyMapping key, boolean critical) {
            this.text = text;
            this.imgIndex = imgIndex;
            this.key = key;
            this.critical = critical;
        }

        public void render(GuiGraphics guiGraphics, int x, int y) {
            Font font = Minecraft.getInstance().font;
            if (critical) {
                int innerWidth = 20 + 4 + font.width(text);
                long currentTime = System.currentTimeMillis();
                if (currentTime % 400 < 200) {
                    guiGraphics.fill(x + 1, y + 1, x + innerWidth + 4 + 1, y + 20 + 1, 0xFF444444);
                    guiGraphics.fill(x, y, x + innerWidth + 4, y + 20, 0xFFDDDD66);
                }
            }
            guiGraphics.blit(ATLAS_LOCATION, x, y, 20, 20,
                    176 + imgIndex * 20, 0, 20, 20, 256, 256);
            if (key != null) {
                guiGraphics.drawCenteredString(font, key.getTranslatedKeyMessage(),
                        x + 10, y + 10 - 4, 0xFFFFECB3);
            }
            guiGraphics.drawString(font, text, x + 20 + 4, y + 10 - 4, 0xFFFFFFFF, true);
        }
    }
}
