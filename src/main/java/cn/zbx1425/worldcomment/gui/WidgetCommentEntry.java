package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.data.network.ImageUrlResolver;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permissions;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WidgetCommentEntry extends AbstractWidget implements IGuiCommon {

    private final CommentEntry comment;
    private final Font font;

    private List<SizedFormattedText> wrappedText = List.of();

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
        int picWidth = (comment.image.sourceUrl.isEmpty() || !showImage) ? 0 : ((width - 20) / 3);
        int textWidth = width - 20 - picWidth - (picWidth > 0 ? 4 : 0);
        wrappedText = SizedFormattedText.splitLines(comment.message, font, textWidth, Style.EMPTY,
            CommentEntry.isMarkerType(comment.messageType), false);
        int textHeight = 0;
        for (SizedFormattedText formattedText : wrappedText) {
            textHeight += (int) (9 * formattedText.sizeModifier);
        }
        int textAreaHeight = 28
                + (comment.message.isEmpty() ? 0 : textHeight)
                + 4;
        int picHeight = 20 + ((comment.image.sourceUrl.isEmpty() || !showImage) ? 0 : (picWidth * 9 / 16)) + 4 + 4;
        height = Math.max(Math.max(textAreaHeight, picHeight), 28 + 4);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiCanvas guiGraphics = ISnGuiCanvas.fromGuiParam(guiParam);

        guiGraphics.blitNineSlicedFast(
                ATLAS_LOCATION, getX(), getY(), getWidth(), getHeight(),
                0, 0, 128, 48, 256, 256,
                24, 4, 4, 28
        );

        int picWidth = (comment.image.sourceUrl.isEmpty() || !showImage) ? 0 : ((width - 20) / 3);
        int picHeight = ((comment.image.sourceUrl.isEmpty() || !showImage) ? 0 : (picWidth * 9 / 16)) + 4;

        if (!comment.message.isEmpty()) {
            int lineY = getY() + 28;
            for (SizedFormattedText formattedText : wrappedText) {
                guiGraphics.pushPose();
                guiGraphics.translate(getX() + 16, lineY, 0);
                guiGraphics.scale(formattedText.sizeModifier, formattedText.sizeModifier);
                guiGraphics.text(font, formattedText.ordered, 0, 0, 0xFF444444, false);
                guiGraphics.popPose();
                lineY += (int) (font.lineHeight * formattedText.sizeModifier);
            }
        }

        if (!comment.image.sourceUrl.isEmpty() && showImage) {
            String thumbUrl = ImageUrlResolver.resolve(comment.image, ImageUrlResolver.ImageUsagePurpose.THUMBNAIL,
                    MainClient.CLIENT_CONFIG.serverIssuedConfig.imageVariants,
                    MainClient.CLIENT_CONFIG.serverIssuedConfig.uploaderCdnConfigs);
            ImageDownload.ImageState imageToDraw = ImageDownload.getTexture(thumbUrl);
            int x1 = getX() + width - 4 - picWidth, x2 = getX() + width - 4;
            int y1 = getY() + 20, y2 = getY() + 20 + picHeight;
            guiGraphics.blit(imageToDraw.getFriendlyTexture(Minecraft.getInstance().getTextureManager()), x1, y1, x2, y2);
        }

        Component nameComponent = comment.initiatorName.isEmpty() ? Component.translatable("gui.worldcomment.anonymous")
                : Component.literal(comment.initiatorName);
        String uuidToDisplay = comment.initiatorName.isEmpty()
                ? (Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_ADMIN) ? comment.initiator.toString() : "")
                : "..." + comment.initiator.toString().substring(24);
        guiGraphics.text(font, nameComponent,
                getX() + 34, getY() + 8, 0xFFFFFFFF, true);

        if (showImage && !comment.initiator.equals(CommentEntry.SYSTEM_MESSAGE_MAGIC_INITIATOR)) {
            String timeStr = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.ROOT)
                    .format(Instant.ofEpochMilli(comment.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime());
            guiGraphics.text(font, timeStr,
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
                            .setStyle(Style.EMPTY.withBold(true) /*.withColor(CommentTypeButton.COMMENT_TYPE_COLOR[comment.messageType - 1] & 0xFFFFFF) */)
                            .append(Component.literal("  (" + comment.location.toShortString() + ")").setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))),
                    Component.literal("  " + Instant.ofEpochMilli(comment.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)),
                    Component.literal("  " + nameComponent.getString() + " " + uuidToDisplay).withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
//? if >=1.20 {
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) { }
//? } else {
    /*public void updateNarration(NarrationElementOutput narrationElementOutput) { }
*///? }

//? if <1.20 {
    /*private int getX() { return x; }
    private int getY() { return y; }
    private void setX(int x) { this.x = x; }
    private void setY(int y) { this.y = y; }
*///? }
}
