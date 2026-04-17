package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.data.network.SubmitDispatcher;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; import com.mojang.blaze3d.vertex.PoseStack; #endif
#if MC_VERSION < "12003" import cn.zbx1425.worldcomment.util.compat.Checkbox; #endif
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommentToolScreen extends Screen implements IGuiCommon {

    private final byte[] imageBytes;
    private final boolean withPlacingDown;

    private final WidgetUnmanagedImage widgetImage;
    private final WidgetEmojiPanel emojiPanel;
    private final MultiLineEditBox textBoxMessage;
    private final WidgetColorButton btnScreenshotConfig;
    private final WidgetSubtleToggleButton checkBoxNoImage;
    private final WidgetSubtleToggleButton checkBoxAnonymous;
    private final WidgetColorButton btnSaveScreenshot;
    private final Button btnSendFeedback;

    boolean screenshotSaved;

    private static final int ROOT_WIDTH = 300;
    private static final int ROOT_HEIGHT = 200;
    private static final int ASIDE_SPACING = 20;

    private static final int ASIDE_WIDTH = (int)((ROOT_WIDTH - ASIDE_SPACING) * 0.35);
    private static final int MAIN_WIDTH = (int)((ROOT_WIDTH - ASIDE_SPACING) * 0.65);
    private static final int MAIN_XOFF = ROOT_WIDTH - MAIN_WIDTH;

    private int rootOffX, rootOffY, asideHeight;

    public CommentToolScreen(byte[] imageBytes, boolean withPlacingDown) {
        super(Component.literal("Comment Tool"));
        this.imageBytes = imageBytes;
        this.withPlacingDown = withPlacingDown;
        this.screenshotSaved = false;
        ByteBuffer offHeapBuffer = OffHeapAllocator.allocate(imageBytes.length);
        try {
            offHeapBuffer.put(imageBytes);
            offHeapBuffer.rewind();
#if MC_VERSION >= "12106"
            this.widgetImage = new WidgetUnmanagedImage(new DynamicTexture(
                () -> Screenshot.getAvailableFile().toPath().toString(), NativeImage.read(offHeapBuffer)));
#else
            this.widgetImage = new WidgetUnmanagedImage(new DynamicTexture(NativeImage.read(offHeapBuffer)));
#endif
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            OffHeapAllocator.free(offHeapBuffer);
        }

        this.emojiPanel = new WidgetEmojiPanel(MAIN_WIDTH, (ROOT_HEIGHT - 14 - 26) / 2, _ -> updateBtnSendFeedback());
        this.textBoxMessage = new MultiLineEditBox.Builder()
            .setPlaceholder(Component.translatable("gui.worldcomment.message"))
            .build(font, MAIN_WIDTH, (ROOT_HEIGHT - 14 - 26) / 2, CommonComponents.EMPTY);
        textBoxMessage.setValue("");
        textBoxMessage.setValueListener(ignored -> updateBtnSendFeedback());
        this.btnSendFeedback = new WidgetColorButton(
            CommentTypeButton.BTN_WIDTH * 2, SQ_SIZE,
            Component.translatable("gui.worldcomment.submit"), 0xFFC5E1A5,
            sender -> sendReport()
        );

        this.checkBoxAnonymous = new WidgetSubtleToggleButton(
            80, 128,
            Component.translatable("gui.worldcomment.anonymous.disable"), Component.translatable("gui.worldcomment.anonymous.enable"),
            false, null
        );
        this.checkBoxNoImage = new WidgetSubtleToggleButton(
            160, 128,
            Component.translatable("gui.worldcomment.exclude_screenshot.disable"), Component.translatable("gui.worldcomment.exclude_screenshot.enable"),
            false, null
        );

        this.btnScreenshotConfig = new WidgetColorButton(
            SQ_SIZE, SQ_SIZE,
            Component.literal(""), 0xFFAAAAAA, sender -> {
            minecraft.setScreen(new ScreenshotConfigScreen());
        });
        btnScreenshotConfig.useIcon(0, 128);
        btnScreenshotConfig.setTooltip(Tooltip.create(Component.translatable("gui.worldcomment.screenshot_config")));

        this.btnSaveScreenshot = new WidgetColorButton(
            ASIDE_WIDTH - 10, SQ_SIZE,
            Component.translatable("gui.worldcomment.save_screenshot"), 0xFF81D4FA, sender -> saveScreenshot());
    }

    @Override
    protected void init() {
        super.init();

        rootOffX = (width - ROOT_WIDTH) / 2;
        rootOffY = (height - ROOT_HEIGHT) / 2;

        emojiPanel.setPosition(rootOffX + MAIN_XOFF, rootOffY + 14);
        textBoxMessage.setPosition(rootOffX + MAIN_XOFF, rootOffY + 14 + (ROOT_HEIGHT - 14 - 26) / 2);
        btnSendFeedback.setPosition(rootOffX + ROOT_WIDTH - CommentTypeButton.BTN_WIDTH * 2 - 5, rootOffY + ROOT_HEIGHT - 3 - 20);
        addRenderableWidget(emojiPanel);
        addRenderableWidget(textBoxMessage);
        addRenderableWidget(btnSendFeedback);

        WidgetMultiLineLabel mainTitle = new WidgetMultiLineLabel(rootOffX + MAIN_XOFF + 5, rootOffY + 3, MAIN_WIDTH - 10, 8,
            Component.translatable("gui.worldcomment.comment_tool.title").withColor(0xFF404040));
        mainTitle.repositionEntries();
        addRenderableWidget(mainTitle);

        updateBtnSendFeedback();

        int baseY = rootOffY;
        widgetImage.setBounds(rootOffX, baseY, ASIDE_WIDTH);
//        addRenderableWidget(widgetImage);
        baseY += widgetImage.getHeight() - 10;
        btnScreenshotConfig.setPosition(rootOffX + ASIDE_WIDTH - 30, baseY);
        addRenderableWidget(btnScreenshotConfig);
        baseY += SQ_SIZE + 8;

        checkBoxAnonymous.setPosition(rootOffX + MAIN_XOFF + 5, rootOffY + ROOT_HEIGHT - 20 - 3);
        addRenderableWidget(checkBoxAnonymous);
        checkBoxNoImage.setPosition(rootOffX + MAIN_XOFF + 5 + 20, rootOffY + ROOT_HEIGHT - 20 - 3);
        addRenderableWidget(checkBoxNoImage);

        WidgetMultiLineLabel lblScreenshotGuide = new WidgetMultiLineLabel(
            rootOffX + 5, baseY, ASIDE_WIDTH - 10, 50,
            Component.translatable("gui.worldcomment.send.guide_just_screenshot").withColor(0xFF404040)
        );
        lblScreenshotGuide.repositionEntries();
        addRenderableWidget(lblScreenshotGuide);
        baseY += lblScreenshotGuide.getHeight() + 5;

        btnSaveScreenshot.setPosition(rootOffX + 5, baseY);
        btnSaveScreenshot.active = !screenshotSaved;
        addRenderableWidget(btnSaveScreenshot);

        baseY += SQ_SIZE;
        asideHeight = baseY - rootOffY + 5;
    }

    @Override
    public void extractRenderState(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        #if MC_VERSION < "12002" renderBackground(guiParam); #endif
        guiGraphics.pushPose();
//        boolean animationDone = setupAnimationTransform(guiGraphics);
//        guiGraphics.translate(0, 0, 1);
        super.extractRenderState(guiParam, mouseX, mouseY, partialTick);
        guiGraphics.popPose();
    }

    @Override
    public void extractBackground(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam
                                 #if MC_VERSION >= "12002", int mouseX, int mouseY, float partialTick #endif) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        super.extractBackground(guiParam #if MC_VERSION >= "12002", mouseX, mouseY, partialTick #endif);
        guiGraphics.pushPose();
//        setupAnimationTransform(guiGraphics);
        guiGraphics.enableBlend();

        guiGraphics.blitNineSlicedFast(
            ATLAS_LOCATION,
            rootOffX - 3, rootOffY - 3, ASIDE_WIDTH + 6, asideHeight + 6,
            80, 58, 40, 40, 256, 256, 4, 4, 4, 4
        );

        guiGraphics.blitNineSlicedFast(
            ATLAS_LOCATION,
            rootOffX + MAIN_XOFF - 3, rootOffY - 3, MAIN_WIDTH + 6, ROOT_HEIGHT + 6,
            80, 58, 40, 40, 256, 256, 4, 4, 4, 4
        );

        widgetImage.extractWidgetRenderState(guiParam, mouseX, mouseY, partialTick);

        guiGraphics.disableBlend();
        guiGraphics.popPose();
    }

    private void sendReport() {
        if (emojiPanel.getSelectedId() == 0) return;
        Minecraft.getInstance().execute(() -> {
            Player player = Minecraft.getInstance().player;
            CommentEntry comment = new CommentEntry(
                    player, checkBoxAnonymous.selected(),
                    emojiPanel.getSelectedId(), textBoxMessage.getValue()
            );
            long jobId = SubmitDispatcher.addJob(
                    comment, checkBoxNoImage.selected() ? null : imageBytes,
                    (job, ex) -> Minecraft.getInstance().execute(() -> {
                        if (job == null) {
                            player.sendSystemMessage(
                                    Component.translatable("gui.worldcomment.send_finish"));
                        } else {
                            if (ex != null) {
                                player.sendSystemMessage(
                                        Component.translatable("gui.worldcomment.send_fail",
                                                ex.getClass().getName() + ": " + ex.getMessage()));
                            } else {
                                player.sendSystemMessage(
                                        Component.translatable("gui.worldcomment.send_upload_incomplete"));
                            }
                        }
                    }
            ));
            if (!withPlacingDown) {
                boolean placedOnGround = SubmitDispatcher.placeJobAtSnapping(
                    jobId,
                    player.blockPosition().atY((int) Math.round(player.position().y - 1.0 / 16)),
                    player.level()
                );
                if (!placedOnGround) {
                    player.sendSystemMessage(
                        Component.translatable("gui.worldcomment.send_in_air"));
                }
            } else {
                player.sendSystemMessage(
                        Component.translatable("gui.worldcomment.send_pending"));
                ItemStack item = CommentToolItem.Client.getHoldingCommentTool();
                if (item != null) {
                    CommentToolItem.setUploadJobId(item, jobId);
                }
            }
        });
        onClose();
    }

    private void saveScreenshot() {
        Path persistentPath = Screenshot.getAvailableFile().toPath();
        try {
            Files.write(persistentPath, imageBytes);
            screenshotSaved = true;
            btnSaveScreenshot.active = false;

            onClose();
        } catch (IOException e) {
            Main.LOGGER.error("Copy image", e);
        }
    }

    @Override
    public void onClose() {
        widgetImage.close();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateBtnSendFeedback() {
        if (emojiPanel.getSelectedId() == 0) {
            btnSendFeedback.active = false;
#if MC_VERSION >= "12000"
            btnSendFeedback.setTooltip(Tooltip.create(Component.translatable("gui.worldcomment.require_comment_type").withStyle(ChatFormatting.RED)));
#endif
        } else if (textBoxMessage.getValue().length() > CommentEntry.MESSAGE_MAX_LENGTH) {
            btnSendFeedback.active = false;
#if MC_VERSION >= "12000"
            btnSendFeedback.setTooltip(Tooltip.create(Component.translatable("gui.worldcomment.message_too_long").withStyle(ChatFormatting.RED)));
#endif
        } else {
            btnSendFeedback.active = true;
#if MC_VERSION >= "12000"
            btnSendFeedback.setTooltip(null);
#endif
        }
    }
}
