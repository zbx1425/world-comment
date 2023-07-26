package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WidgetCommentEntry extends AbstractWidget implements IGuiCommon {

    private final CommentEntry comment;
    private final Font font;

    public static final int TOP_SINK = 12;

    public WidgetCommentEntry(CommentEntry comment) {
        super(0, 0, 0, 0, Component.literal(comment.message));
        this.comment = comment;
        this.font = Minecraft.getInstance().font;
        calculateHeight();
    }

    public void setBounds(int x, int y, int width) {
        setX(x);
        setY(y);
        setWidth(width);
        calculateHeight();
    }

    private void calculateHeight() {
        int picWidth = (width - 16) / 3;
        int textHeight = 26
                + (comment.message.isEmpty() ? 0 : font.wordWrapHeight(comment.message, picWidth * 2 - 8))
                + 4;
        int picHeight = 20 + (comment.image.url.isEmpty() ? 0 : (picWidth * 9 / 16)) + 4;
        height = Math.max(Math.max(textHeight, picHeight), 28 + 4);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        graphicsBlit9(
                guiGraphics, getX(), getY(), getWidth(), getHeight(),
                0, 0, 128, 48, 256, 256,
                24, 4, 4, 28
        );

        if (!comment.message.isEmpty()) {
            int picWidth = (width - 16) / 3;
            guiGraphics.drawWordWrap(font, FormattedText.of(comment.message), getX() + 16, getY() + 26,
                    picWidth * 2 - 8, 0xFF444444);
        }

        if (!comment.image.url.isEmpty()) {
            int picWidth = (width - 16) / 3;
            int picHeight = picWidth * 9 / 16;
            String imageUrl = comment.image.thumbUrl.isEmpty() ? comment.image.url : comment.image.thumbUrl;
            RenderSystem.setShaderTexture(0, ImageDownload.getTexture(imageUrl).getId());
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            int x1 = getX() + width - 4 - picWidth, x2 = getX() + width - 4;
            int y1 = getY() + 20, y2 = getY() + 20 + picHeight;
            bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(0, 0).endVertex();
            bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(0, 1).endVertex();
            bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(1, 1).endVertex();
            bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(1, 0).endVertex();
            BufferUploader.drawWithShader(bufferBuilder.end());
        }

        guiGraphics.drawString(font,
                comment.initiatorName.isEmpty() ? Component.translatable("gui.worldcomment.anonymous")
                        : Component.literal(comment.initiatorName),
                getX() + 34, getY() + 8, 0xFFFFFFFF, true
        );
        String timeStr = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.ROOT)
                .format(Instant.ofEpochMilli(comment.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime());
        guiGraphics.drawString(font, timeStr,
                getX() + getWidth() - 6 - font.width(timeStr), getY() + 8, 0xFFBBBBBB, true);

        RenderSystem.enableBlend();
        guiGraphics.blit(ATLAS_LOCATION, getX() + 6, getY() + 2, 18, 18,
                ((comment.messageType - 1) % 4) * 64, (int)((comment.messageType - 1) / 4) * 64 + 128, 64, 64, 256, 256);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
