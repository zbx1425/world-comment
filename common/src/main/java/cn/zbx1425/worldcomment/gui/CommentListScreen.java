package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import cn.zbx1425.worldcomment.network.PacketEntryActionC2S;
import cn.zbx1425.worldcomment.util.FrameTask;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; #endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import java.util.*;

public class CommentListScreen extends Screen implements IGuiCommon {

    // ---- Tab enum ----

    enum Tab {
        NEARBY, RECENT, MY_POSTS, DETAIL
    }

    // ---- Sub-view interface ----

    private interface SubView {
        void onEnter();
        void render(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam,
                    ISnGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
        boolean handleClick(double mouseX, double mouseY);
        boolean handleScroll(int scrollAmount);
    }

    // ---- State ----

    private Tab currentTab = Tab.NEARBY;
    private Tab returnTab = Tab.NEARBY;

    private final net.minecraft.client.gui.components.Button[] pageButtons = new net.minecraft.client.gui.components.Button[4];

    final List<CommentEntry> commentList = new ArrayList<>();
    int commentListOffset = 0;
    int latestCommentsRequestedAmount = 0;
    private static final int LATEST_PAGE_SIZE = 20;

    CommentEntry commentForDetail;
    private CommentEntry commentToDelete;

    private final Map<CommentEntry, WidgetCommentEntry> widgets = new WeakHashMap<>();
    private long lastRequestNonce;
    private double accumulatedScroll = 0;

    // ---- Layout (refreshed in init) ----

    private int listAndLeftWidth, listWidth, xAsideLeftL, xListL, xAsideRightL;

    private static final int LIST_WIDTH_PREFERRED = 300;
    private static final int ASIDE_L_WIDTH = 100;
    private static final int ASIDE_R_WIDTH = 50;

    private void updateLayout() {
        if (width < ASIDE_L_WIDTH + 2 + LIST_WIDTH_PREFERRED + 2 + ASIDE_R_WIDTH) {
            // Very narrow, shrink the center list
            listAndLeftWidth = width - ASIDE_R_WIDTH;
            xAsideLeftL = 0;
            xAsideRightL = width - ASIDE_R_WIDTH;
        } else {
            // Center the (ASIDE_WIDTH_LEFT + 2 + LIST_WIDTH_PREFERRED + 2) part
            listAndLeftWidth = (ASIDE_L_WIDTH + 2 + LIST_WIDTH_PREFERRED + 2);
            xAsideLeftL = (width - (listAndLeftWidth + ASIDE_R_WIDTH)) / 2;
            xAsideRightL = xAsideLeftL + ASIDE_L_WIDTH + 2 + LIST_WIDTH_PREFERRED + 2;
        }
        xListL = xAsideLeftL + ASIDE_L_WIDTH + 2;
        listWidth = listAndLeftWidth - ASIDE_L_WIDTH - 2 - 2;
    }

    // ---- Sub-views ----

    private final ListSubView listView = new ListSubView();
    private final DetailSubView detailView = new DetailSubView();

    private SubView getCurrentView() {
        return currentTab == Tab.DETAIL ? detailView : listView;
    }

    // ---- Helpers ----

    private WidgetCommentEntry getWidget(CommentEntry entry) {
        return widgets.computeIfAbsent(entry, WidgetCommentEntry::new);
    }

    private boolean canDelete(CommentEntry comment) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN)
                || mc.player.getGameProfile().id().equals(comment.initiator);
    }

    private boolean tryDelete(CommentEntry comment) {
        if (comment == commentToDelete) {
            PacketEntryActionC2S.ClientLogics.send(comment, PacketEntryActionC2S.ACTION_DELETE);
            commentList.remove(comment);
            commentToDelete = null;
            commentListOffset = Mth.clamp(commentListOffset, 0, Math.max(commentList.size() - 1, 0));
            return true;
        } else {
            commentToDelete = comment;
            return false;
        }
    }

    private static void renderIcon(ISnGuiGraphics g, int x, int y, int size, int u, int v, double mx, double my) {
        g.blit(ATLAS_LOCATION, x, y, size, size, u, v, 20, 20, 256, 256);
        if (mx > x && mx < x + size && my > y && my < y + size) {
            g.blit(ATLAS_LOCATION, x, y, size, size, 236, 60, 20, 20, 256, 256);
        }
    }

    private static boolean hitTest(double mx, double my, int x, int y, int size) {
        return mx > x && mx < x + size && my > y && my < y + size;
    }

    // ---- Constructor & Navigation ----

    protected CommentListScreen(CommentEntry commentForDetail) {
        super(Component.literal(""));
        this.commentForDetail = commentForDetail;
        Tab initialTab = commentForDetail != null ? Tab.DETAIL : Tab.NEARBY;
        currentTab = initialTab;
        returnTab = initialTab;
        getCurrentView().onEnter();
    }

    void switchTo(Tab tab) {
        Tab previousTab = currentTab;
        if (tab == Tab.DETAIL) {
            returnTab = currentTab;
        }
        currentTab = tab;
        if (previousTab != Tab.DETAIL || commentList.isEmpty()) {
            getCurrentView().onEnter();
        }
        init();
    }

    // ---- Screen lifecycle ----

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        updateLayout();

        pageButtons[0] = addRenderableWidget(new WidgetColorButton(xAsideLeftL + 10, 40, 80, 20,
                Component.translatable("gui.worldcomment.list.nearby_posts"), 0xffe57373, sender -> switchTo(Tab.NEARBY)));
        pageButtons[1] = addRenderableWidget(new WidgetColorButton(xAsideLeftL + 10, 64, 80, 20,
                Component.translatable("gui.worldcomment.list.recent_posts"), 0xffe57373, sender -> switchTo(Tab.RECENT)));
        pageButtons[2] = addRenderableWidget(new WidgetColorButton(xAsideLeftL + 10, 88, 80, 20,
                Component.translatable("gui.worldcomment.list.my_posts"), 0xffe57373, sender -> switchTo(Tab.MY_POSTS)));
        pageButtons[3] = addRenderableWidget(new WidgetColorButton(xAsideLeftL + 10, 122, 80, 20,
                Component.translatable("gui.worldcomment.list.detail"), 0xffe57373, sender -> {}));

        int tabIndex = currentTab.ordinal();
        for (int i = 0; i < pageButtons.length; i++) {
            pageButtons[i].active = i != tabIndex;
        }
        pageButtons[3].visible = currentTab == Tab.DETAIL;
    }

    @Override
    public void extractRenderState(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);

        Minecraft minecraft = Minecraft.getInstance();
        #if MC_VERSION < "12002" renderBackground(guiParam); #endif
        #if MC_VERSION >= "12100" super.extractRenderState(guiParam, mouseX, mouseY, partialTick); #endif
        guiGraphics.pushPose();
        guiGraphics.translate(0, 0, 1);

        guiGraphics.drawString(minecraft.font, Component.translatable("gui.worldcomment.list.title"),
                xAsideLeftL + 10, 15, 0xFFFFE6C0, true);
        for (net.minecraft.client.gui.components.Button button : pageButtons) {
            button #if MC_VERSION >= "11903" .setX #else .x = #endif (xAsideLeftL + 10);
        }

        getCurrentView().render(guiParam, guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.popPose();
        #if MC_VERSION < "12100" super.render(guiParam, mouseX, mouseY, partialTick); #endif
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (getCurrentView().handleClick(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    private static final Identifier INWORLD_MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

    @Override
    public void extractBackground(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam
                                 #if MC_VERSION >= "12002", int mouseX, int mouseY, float partialTick #endif) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(guiParam);
        super.extractBackground(guiParam #if MC_VERSION >= "12002", mouseX, mouseY, partialTick #endif);

        guiGraphics.enableBlend();

        guiParam.fill(xAsideLeftL, 0, xAsideLeftL + ASIDE_L_WIDTH, 70, 0x33e57373);
        guiParam.fillGradient(xAsideLeftL, 70, xAsideLeftL + ASIDE_L_WIDTH, 140, 0x33e57373, 0x00e57373);

        guiParam.fill(xListL - 2, 0, xListL - 1, height, 0x33FFFFFF);
        guiParam.fill(xListL - 1, 0, xListL, height, 0xBF000000);

        guiParam.fill(xAsideRightL - 2, 0, xAsideRightL - 1, height, 0xBF000000);
        guiParam.fill(xAsideRightL - 1, 0, xAsideRightL, height, 0x33FFFFFF);

        guiGraphics.disableBlend();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY #if MC_VERSION >= "12002", double deltaX #endif, double deltaY) {
        if (this.accumulatedScroll != 0.0 && Math.signum(deltaY) != Math.signum(this.accumulatedScroll)) {
            this.accumulatedScroll = 0.0;
        }
        this.accumulatedScroll += deltaY;
        int scrollAmount = (int)this.accumulatedScroll;
        if (scrollAmount == 0) return super.mouseScrolled(mouseX, mouseY #if MC_VERSION >= "12002", deltaX #endif, deltaY);
        this.accumulatedScroll -= scrollAmount;

        if (getCurrentView().handleScroll(scrollAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY #if MC_VERSION >= "12002", deltaX #endif, deltaY);
    }

    @Override
    public void onClose() {
        if (currentTab == Tab.DETAIL && returnTab != Tab.DETAIL) {
            switchTo(returnTab);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return false; // For the blurred background
    }

    // ---- Public API ----

    public void handleCommentDataUI(List<CommentEntry> data, long nonce) {
        if (nonce != lastRequestNonce) return;
        commentList.addAll(data);
        commentList.sort(Comparator.comparingLong(entry -> -entry.timestamp));
        commentListOffset = Mth.clamp(commentListOffset, 0, Math.max(commentList.size() - 1, 0));
    }

    public static void triggerOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        FrameTask.enqueue(() -> {
            if (minecraft.screen instanceof CommentListScreen) {
                minecraft.screen.onClose();
            } else {
                minecraft.setScreen(new CommentListScreen(null));
            }
        }, 1);
    }

    public static boolean handleKeyTab() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        if (ClientRayPicking.pickedComments.isEmpty()) return false;

        minecraft.execute(() -> {
            if (minecraft.screen instanceof CommentListScreen) {
                minecraft.screen.onClose();
            } else if (minecraft.screen == null) {
                minecraft.setScreen(
                        new CommentListScreen(ClientRayPicking.pickedComments.get(ClientRayPicking.overlayOffset)));
            }
        });
        return true;
    }

    // ======== Sub-view: Comment List (NEARBY / RECENT / MY_POSTS) ========

    private class ListSubView implements SubView {

        @Override
        public void onEnter() {
            Minecraft minecraft = Minecraft.getInstance();
            commentList.clear();
            commentListOffset = 0;
            switch (currentTab) {
                case NEARBY -> {
                    BlockPos playerPos = minecraft.player.blockPosition();
                    for (Map<BlockPos, List<CommentEntry>> region : ClientWorldData.INSTANCE.regions.values()) {
                        for (Map.Entry<BlockPos, List<CommentEntry>> blockData : region.entrySet()) {
                            for (CommentEntry comment : blockData.getValue()) {
                                if (comment.deleted) continue;
                                commentList.add(comment);
                            }
                        }
                    }
                    commentList.sort(Comparator.comparingDouble(entry -> entry.location.distSqr(playerPos)));
                }
                case RECENT -> {
                    lastRequestNonce = ServerWorldData.SNOWFLAKE.nextId();
                    latestCommentsRequestedAmount = 0;
                    PacketCollectionRequestC2S.ClientLogics.sendLatest(
                            latestCommentsRequestedAmount, LATEST_PAGE_SIZE, lastRequestNonce);
                    latestCommentsRequestedAmount += LATEST_PAGE_SIZE;
                }
                case MY_POSTS -> {
                    lastRequestNonce = ServerWorldData.SNOWFLAKE.nextId();
                    PacketCollectionRequestC2S.ClientLogics.sendPlayer(
                            minecraft.player.getGameProfile().id(), lastRequestNonce);
                }
                default -> {}
            }
        }

        @Override
        public void render(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam,
                           ISnGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiParam.blit(RenderPipelines.GUI_TEXTURED, INWORLD_MENU_LIST_BACKGROUND,
                xListL, 0, 0, commentListOffset,
                listWidth, height, 32, 32);
//            guiGraphics.enableScissor(0, 0, width, height);

            int yOffset = 20;
            for (int i = commentListOffset; i < commentList.size(); i++) {
                CommentEntry comment = commentList.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                widget.showImage = true;
                widget.setBounds(xListL + 5, yOffset, listWidth - 10);
                widget.extractRenderState(guiParam, mouseX, mouseY, partialTick);

                int iconX = xAsideRightL + 5;
                renderIcon(guiGraphics, iconX, yOffset + 4, 16, 196, 60, mouseX, mouseY);

                if (canDelete(comment)) {
                    renderIcon(guiGraphics, iconX, yOffset + 4 + 16, 16, 216, 60, mouseX, mouseY);
                    if (hitTest(mouseX, mouseY, iconX, yOffset + 4 + 16, 16) && commentToDelete == comment) {
                        guiGraphics.renderTooltip(font, Component.translatable("gui.worldcomment.list.remove.confirm"), mouseX, mouseY);
                    }
                }

                yOffset += widget.getHeight() + 6;
                if (yOffset > height - 22) break;
            }
//            guiGraphics.disableScissor();

            if (commentList.size() > 1) {
                String pageStr = String.format("↕ %d / %d", commentListOffset + 1, commentList.size());
                guiGraphics.drawString(Minecraft.getInstance().font, pageStr,
                    xAsideRightL + 5,
                        5, 0xFFA5D6A7, true);
            } else if (commentList.isEmpty()) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                        Component.translatable("gui.worldcomment.list.empty"),
                        xListL + listWidth / 2, height / 2 - 5, 0xFFA5D6A7);
            }
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY) {
            int yOffset = 20;
            for (int i = commentListOffset; i < commentList.size(); i++) {
                CommentEntry comment = commentList.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                int iconX = xAsideRightL + 5;

                if (hitTest(mouseX, mouseY, iconX, yOffset + 4, 16)) {
                    commentForDetail = comment;
                    switchTo(Tab.DETAIL);
                    return true;
                }

                if (canDelete(comment) && hitTest(mouseX, mouseY, iconX, yOffset + 4 + 16, 16)) {
                    tryDelete(comment);
                    return true;
                }

                yOffset += widget.getHeight() + 6;
                if (yOffset > height - 22) break;
            }
            return false;
        }

        @Override
        public boolean handleScroll(int scrollAmount) {
            if (commentList.size() <= 1) {
                commentListOffset = 0;
                return false;
            }
            int dir = -(int)Math.signum(scrollAmount);
            commentListOffset = Mth.clamp(commentListOffset + dir, 0, Math.max(commentList.size() - 1, 0));

            if (currentTab == Tab.RECENT && commentListOffset >= latestCommentsRequestedAmount - LATEST_PAGE_SIZE / 2) {
                lastRequestNonce = ServerWorldData.SNOWFLAKE.nextId();
                PacketCollectionRequestC2S.ClientLogics.sendLatest(
                        latestCommentsRequestedAmount, LATEST_PAGE_SIZE, lastRequestNonce);
                latestCommentsRequestedAmount += LATEST_PAGE_SIZE;
            }
            return true;
        }
    }

    // ======== Sub-view: Comment Detail ========

    private class DetailSubView implements SubView {

        @Override
        public void onEnter() {
        }

        @Override
        public void render(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam,
                           ISnGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            CommentEntry comment = commentForDetail;

            int maxPicWidth = width - 100 - 20 - 20;
            int maxPicHeight = height - 30 - 20 - 20;

            ImageDownload.ImageState imageToDraw = ImageDownload.getTexture(comment.image, false);
            int picWidth = Math.min(maxPicWidth, maxPicHeight * imageToDraw.width / imageToDraw.height);
            int picHeight = picWidth * imageToDraw.height / imageToDraw.width;
            int x1 = 100 + 10, x2 = 100 + 10 + picWidth;
            int y1 = 30 + 10, y2 = 30 + 10 + picHeight;
            guiGraphics.fill(x1 + 3, y1 + 3, x2 + 3, y2 + 3, 0xFF000000);
            guiGraphics.blit(imageToDraw.getFriendlyTexture(minecraft.getTextureManager()), x1, y1, x2, y2);

            WidgetCommentEntry widget = getWidget(comment);
            widget.showImage = false;
            int imgAreaWidth = width - 100 - 20 - 10;
            widget.setBounds(100 + 10 + imgAreaWidth - (imgAreaWidth / 2), 0, imgAreaWidth / 2);
            widget.setBounds(100 + 10 + imgAreaWidth - (imgAreaWidth / 2), height - 20 - widget.getHeight(),
                    imgAreaWidth / 2);
            widget.extractRenderState(guiParam, mouseX, mouseY, partialTick);

            if (canDelete(comment)) {
                int deleteBtnX = 100 + 18, deleteBtnY = height - 20 - 22;
                renderIcon(guiGraphics, deleteBtnX, deleteBtnY, 20, 216, 60, mouseX, mouseY);
                if (hitTest(mouseX, mouseY, deleteBtnX, deleteBtnY, 20) && commentToDelete == comment) {
                    guiGraphics.renderTooltip(font, Component.translatable("gui.worldcomment.list.remove.confirm"), mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY) {
            CommentEntry comment = commentForDetail;
            if (canDelete(comment)) {
                int deleteBtnX = 100 + 18, deleteBtnY = height - 20 - 22;
                if (hitTest(mouseX, mouseY, deleteBtnX, deleteBtnY, 20)) {
                    if (tryDelete(comment)) {
                        onClose();
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleScroll(int scrollAmount) {
            return false;
        }
    }
}
