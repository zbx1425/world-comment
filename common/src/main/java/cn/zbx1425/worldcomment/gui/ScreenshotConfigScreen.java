package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ScreenshotConfigScreen extends Screen implements IGuiCommon {

    private boolean prevHideGui;
    private boolean prevHideComments;

    private static final int CONTAINER_PADDING_X = 8;
    private static final int CONTAINER_PADDING_Y = 5;
    public int containerWidth, containerHeight, containerOffsetX, containerOffsetY;

    private Checkbox cbIncludeHud, cbIncludeComments;
    private Button btnOk;

    public ScreenshotConfigScreen() {
        super(Component.literal("Screenshot Config"));
        prevHideGui = Minecraft.getInstance().options.hideGui;
        prevHideComments = !MainClient.CLIENT_CONFIG.isCommentVisible;
    }

    @Override
    protected void init() {
        super.init();
        Screenshot.applyClientConfigForScreenshot();

        int baseY = CONTAINER_PADDING_Y;
        cbIncludeHud = Checkbox
                .builder(Component.translatable("gui.worldcomment.config.screenshot_hud"), minecraft.font)
                .pos(0, baseY).selected(MainClient.CLIENT_CONFIG.screenshotIncludeGui)
                .onValueChange((sender, value) -> {
                    MainClient.CLIENT_CONFIG.screenshotIncludeGui = value;
                    Screenshot.applyClientConfigForScreenshot();
                }).build();
        addRenderableWidget(cbIncludeHud);
        baseY += SQ_SIZE;
        cbIncludeComments = Checkbox
                .builder(Component.translatable("gui.worldcomment.config.screenshot_comments"), minecraft.font)
                .pos(0, baseY).selected(MainClient.CLIENT_CONFIG.screenshotIncludeComments)
                .onValueChange((sender, value) -> {
                    MainClient.CLIENT_CONFIG.screenshotIncludeComments = value;
                    Screenshot.applyClientConfigForScreenshot();
                }).build();
        addRenderableWidget(cbIncludeComments);
        baseY += SQ_SIZE;
        baseY += CONTAINER_PADDING_Y;

        containerWidth = 200;
        btnOk = new WidgetColorButton(
                containerWidth - CommentTypeButton.BTN_WIDTH * 2, baseY, CommentTypeButton.BTN_WIDTH * 2, SQ_SIZE,
                Component.translatable("gui.ok"), 0xFFC5E1A5,
                sender -> onClose()
        );
        addRenderableWidget(btnOk);

        containerHeight = btnOk #if MC_VERSION >= "11903" .getY() #else .y #endif + btnOk.getHeight();

        containerOffsetX = Math.max((width / 2 - (containerWidth + CONTAINER_PADDING_X * 2)) / 2 + CONTAINER_PADDING_X, 20);
        containerOffsetY = (height - (containerHeight + CONTAINER_PADDING_Y * 2)) / 2 + CONTAINER_PADDING_Y;
        for (GuiEventListener child : children()) {
            AbstractWidget widget = (AbstractWidget)child;
            widget #if MC_VERSION >= "11903" .setX #else .x = #endif (widget #if MC_VERSION >= "11903" .getX() #else .x #endif + containerOffsetX);
            widget #if MC_VERSION >= "11903" .setY #else .y = #endif (widget #if MC_VERSION >= "11903" .getY() #else .y #endif + containerOffsetY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        guiGraphics.fill(
                containerOffsetX - CONTAINER_PADDING_X,
                containerOffsetY - CONTAINER_PADDING_Y,
                containerOffsetX + containerWidth + CONTAINER_PADDING_X,
                containerOffsetY + containerHeight + CONTAINER_PADDING_Y,
                0x99222222
        );
        guiGraphics.fill(
                containerOffsetX - CONTAINER_PADDING_X,
                containerOffsetY + containerHeight - SQ_SIZE + CONTAINER_PADDING_Y,
                containerOffsetX + containerWidth + CONTAINER_PADDING_X,
                containerOffsetY + containerHeight + CONTAINER_PADDING_Y,
                0x66546e7a
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.options.hideGui = prevHideGui;
        MainClient.CLIENT_CONFIG.isCommentVisible = !prevHideComments;
        Screenshot.isGrabbing = false;
        super.onClose();
    }
}
