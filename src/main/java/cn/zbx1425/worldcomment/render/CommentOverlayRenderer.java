package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.WidgetCommentEntry;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import net.minecraft.client.Minecraft;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

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
            widget.setBounds(width / 2 + 10, yOffset, Math.min(width / 2 - 30, 250));
            yOffset += widget.getHeight() + 10;
            cachedWidgets.add(widget);
        }
    }

    public static void render(ISnGuiCanvas guiGraphics) {
        if (cachedWidth != guiGraphics.guiWidth() || !ClientRayPicking.pickedComments.equals(cachedComments)) {
            calculateLayout(guiGraphics.guiWidth());
            cachedWidth = guiGraphics.guiWidth();
        }
        if (!cachedComments.isEmpty()) {
            guiGraphics.pushPose();
            WidgetCommentEntry pickedWidget = cachedWidgets.get(ClientRayPicking.overlayOffset);

            //? if >=1.19.3 {
            int baseYOffset = guiGraphics.guiHeight() / 2
                    - (pickedWidget.getY() + WidgetCommentEntry.TOP_SINK);
            //?} else {
            /*int baseYOffset = guiGraphics.guiHeight() / 2
                    - (pickedWidget.y + WidgetCommentEntry.TOP_SINK);
            *///?}

            // Make sure the picked comment is fully visible
            if (baseYOffset + pickedWidget.getY() + pickedWidget.getHeight() + 10 > guiGraphics.guiHeight()) {
                baseYOffset = guiGraphics.guiHeight() - 10 - pickedWidget.getHeight() - pickedWidget.getY();
            }

            guiGraphics.translate(0, baseYOffset, 0);
            for (WidgetCommentEntry widget : cachedWidgets) {
                //? if >=1.19.3 {
                if (widget.getY() + baseYOffset + widget.getHeight() > 0
                        && widget.getY() + baseYOffset < guiGraphics.guiHeight()) {
                //?} else {
                /*if (widget.y + baseYOffset + widget.getHeight() > 0
                        && widget.y + baseYOffset < guiGraphics.guiHeight()) {
                *///?}
                    widget.extractRenderState(guiGraphics.getGuiParam(), 0, 0, 0);
                }
            }
            guiGraphics.popPose();
            if (cachedComments.size() > 1) {
                String pageStr = String.format("↕ %d / %d", ClientRayPicking.overlayOffset + 1, cachedComments.size());
                guiGraphics.text(Minecraft.getInstance().font, pageStr,
                        guiGraphics.guiWidth() / 2 - 10 - Minecraft.getInstance().font.width(pageStr),
                        guiGraphics.guiHeight() / 2 - 8 / 2, 0xFFA5D6A7, true);
            }
        }
    }
}
