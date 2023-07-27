package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.WidgetCommentEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CommentOverlayRenderer {

    private static final List<CommentEntry> cachedComments = new ArrayList<>();
    private static final List<WidgetCommentEntry> cachedWidgets = new ArrayList<>();
    private static int cachedWidth = 0;

    private static void calculateLayout(int width) {
        cachedComments.clear();
        cachedComments.addAll(ClientRayPicking.pickedComments);
        cachedWidgets.clear();
        int yOffset = 0;
        for (CommentEntry comment : cachedComments) {
            WidgetCommentEntry widget = new WidgetCommentEntry(comment);
            widget.setBounds(width / 2 + 10, yOffset, width / 2 - 30);
            yOffset += widget.getHeight() + 10;
            cachedWidgets.add(widget);
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        if (cachedWidth != guiGraphics.guiWidth() || !ClientRayPicking.pickedComments.equals(cachedComments)) {
            calculateLayout(guiGraphics.guiWidth());
            cachedWidth = guiGraphics.guiWidth();
        }
        if (cachedComments.size() > 0) {
            guiGraphics.pose().pushPose();
            int baseYOffset = guiGraphics.guiHeight() / 2
                    - (cachedWidgets.get(ClientRayPicking.overlayOffset).getY() + WidgetCommentEntry.TOP_SINK);
            guiGraphics.pose().translate(0, baseYOffset, 0);
            for (WidgetCommentEntry widget : cachedWidgets) {
                if (widget.getY() + baseYOffset + widget.getHeight() > 0
                        && widget.getY() + baseYOffset < guiGraphics.guiHeight()) {
                    widget.render(guiGraphics, 0, 0, 0);
                }
            }
            guiGraphics.pose().popPose();
            if (cachedComments.size() > 1) {
                String pageStr = String.format("↕ %d / %d", ClientRayPicking.overlayOffset + 1, cachedComments.size());
                guiGraphics.drawString(Minecraft.getInstance().font, pageStr,
                        guiGraphics.guiWidth() / 2 - 10 - Minecraft.getInstance().font.width(pageStr),
                        guiGraphics.guiHeight() / 2 - 8 / 2, 0xFFA5D6A7, true);
            }
        }
    }
}
