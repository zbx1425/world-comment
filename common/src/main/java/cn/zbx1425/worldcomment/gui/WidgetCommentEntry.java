package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; #endif
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.server.permissions.Permissions;
#if MC_VERSION >= "11903" import org.joml.Matrix4f; #else import com.mojang.math.Matrix4f; #endif

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WidgetCommentEntry extends AbstractWidget implements IGuiCommon {

    private final CommentEntry comment;
    private final Font font;

    private List<FormattedCharSequence> wrappedText = List.of();

    public boolean showImage = true;

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
        int picWidth = (comment.image.url.isEmpty() || !showImage) ? 0 : ((width - 20) / 3);
        int textWidth = width - 20 - picWidth - (picWidth > 0 ? 4 : 0);
        wrappedText = Language.getInstance().getVisualOrder(
                font.getSplitter().splitLines(comment.message, textWidth, Style.EMPTY));
        int textHeight = 26
                + (comment.message.isEmpty() ? 0 : 9 * wrappedText.size())
                + 4;
        int picHeight = 20 + ((comment.image.url.isEmpty() || !showImage) ? 0 : (picWidth * 9 / 16)) + 4 + 4;
        height = Math.max(Math.max(textHeight, picHeight), 28 + 4);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);

        guiGraphics.blitNineSlicedFast(
                ATLAS_LOCATION, getX(), getY(), getWidth(), getHeight(),
                0, 0, 128, 48, 256, 256,
                24, 4, 4, 28
        );

        int picWidth = (comment.image.url.isEmpty() || !showImage) ? 0 : ((width - 20) / 3);
        int picHeight = ((comment.image.url.isEmpty() || !showImage) ? 0 : (picWidth * 9 / 16)) + 4;

        if (!comment.message.isEmpty()) {
            int lineY = getY() + 26;
            for (FormattedCharSequence formattedCharSequence : wrappedText) {
                guiGraphics.drawString(font, formattedCharSequence, getX() + 16, lineY, 0xFF444444, false);
                lineY += font.lineHeight;
            }
        }

        if (!comment.image.url.isEmpty() && showImage) {
            ImageDownload.ImageState imageToDraw = ImageDownload.getTexture(comment.image, true);
            int x1 = getX() + width - 4 - picWidth, x2 = getX() + width - 4;
            int y1 = getY() + 20, y2 = getY() + 20 + picHeight;
            guiGraphics.blit(imageToDraw.getFriendlyTexture(Minecraft.getInstance().getTextureManager()), x1, y1, x2, y2);
        }

        Component nameComponent = comment.initiatorName.isEmpty() ? Component.translatable("gui.worldcomment.anonymous")
                : Component.literal(comment.initiatorName);
        String uuidToDisplay = comment.initiatorName.isEmpty()
                ? (Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_ADMIN) ? comment.initiator.toString() : "")
                : "..." + comment.initiator.toString().substring(24);
        guiGraphics.drawString(font, nameComponent,
                getX() + 34, getY() + 8, 0xFFFFFFFF, true);

        if (showImage) {
            String timeStr = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.ROOT)
                    .format(Instant.ofEpochMilli(comment.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime());
            guiGraphics.drawString(font, timeStr,
                    getX() + getWidth() - 6 - font.width(timeStr), getY() + 8, 0xFFBBBBBB, true);
        }

        guiGraphics.enableBlend();
        TextureAtlasSprite iconSprite = EmojiRegistry.INSTANCE.getSprite(comment.messageType);
        guiParam.pose().pushMatrix();
        guiParam.pose().translate(0.5f, 0.5f);
        guiParam.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, getX() + 7, getY() + 3, 18, 18, 0xFF404040);
        guiParam.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, getX() + 6, getY() + 2, 18, 18);
        guiParam.pose().popMatrix();
        guiGraphics.disableBlend();

        if (mouseX > getX() + 4 && mouseX < getX() + getWidth() && mouseY > getY() && mouseY < getY() + 24) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.translatable("gui.worldcomment.comment_type." + comment.messageType)
                            .setStyle(Style.EMPTY.withBold(true).withColor(CommentTypeButton.COMMENT_TYPE_COLOR[comment.messageType - 1] & 0xFFFFFF))
                            .append(Component.literal("  (" + comment.location.toShortString() + ")").setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))),
                    Component.literal("  " + Instant.ofEpochMilli(comment.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)),
                    Component.literal("  " + nameComponent.getString() + " " + uuidToDisplay).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
#if MC_VERSION >= "12000"
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
#else
    public void updateNarration(NarrationElementOutput narrationElementOutput) { }
#endif

#if MC_VERSION < "12000"
    private int getX() { return x; }
    private int getY() { return y; }
    private void setX(int x) { this.x = x; }
    private void setY(int y) { this.y = y; }
#endif
}
