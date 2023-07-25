package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.WidgetCommentEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.WeakHashMap;

public class CommentOverlayRenderer {

    private static final Map<CommentEntry, WidgetCommentEntry> widgets = new WeakHashMap<>();

    private static WidgetCommentEntry getWidget(CommentEntry entry) {
        return widgets.computeIfAbsent(entry, WidgetCommentEntry::new);
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        int pickedCommentsSize = ClientRayPicking.pickedComments.size();
        if (pickedCommentsSize > 0) {
            int[] yOffsets = new int[pickedCommentsSize];
            for (int i = 0; i < pickedCommentsSize; i++) {
                CommentEntry comment = ClientRayPicking.pickedComments.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                widget.setBounds(
                        guiGraphics.guiWidth() / 2 + 10,
                        yOffsets[i],
                        guiGraphics.guiWidth() / 2 - 30
                );
                if (i < pickedCommentsSize - 1) {
                    yOffsets[i + 1] = yOffsets[i] + widget.getHeight() + 10;
                }
            }
            int baseYOffset = guiGraphics.guiHeight() / 2
                    - (yOffsets[ClientRayPicking.overlayOffset] + WidgetCommentEntry.TOP_SINK);
            for (int i = 0; i < pickedCommentsSize; i++) {
                CommentEntry comment = ClientRayPicking.pickedComments.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                widget.setBounds(
                        guiGraphics.guiWidth() / 2 + 10,
                        yOffsets[i] + baseYOffset,
                        guiGraphics.guiWidth() / 2 - 30
                );
                if (widget.getY() + widget.getHeight() > 0 && widget.getY() < guiGraphics.guiHeight()) {
                    widget.render(guiGraphics, 0, 0, partialTick);
                }
            }
            if (pickedCommentsSize > 1) {
                String pageStr = String.format("↕ %d / %d", ClientRayPicking.overlayOffset + 1, pickedCommentsSize);
                guiGraphics.drawString(Minecraft.getInstance().font, pageStr,
                        guiGraphics.guiWidth() / 2 - 10 - Minecraft.getInstance().font.width(pageStr),
                        guiGraphics.guiHeight() / 2 - 8 / 2, 0xFFA5D6A7, true);
            }
        }
    }
}
