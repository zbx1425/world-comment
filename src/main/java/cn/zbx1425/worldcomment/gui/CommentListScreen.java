package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.data.network.ImageUrlResolver;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import cn.zbx1425.worldcomment.network.PacketEntryActionC2S;
import cn.zbx1425.worldcomment.util.FrameTask;
import net.minecraft.client.Minecraft;
//? if >=1.20
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <1.20
//import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Style;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import java.util.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CommentListScreen extends Screen implements IGuiCommon {

    // ---- Tab enum ----

    enum Tab {
        NEARBY, RECENT, MY_POSTS, DETAIL
    }

    // ---- Sub-view interface ----

    private interface SubView {
        void onEnter();
        void render(GuiGraphicsExtractor guiParam,
                    ISnGuiCanvas guiGraphics, int mouseX, int mouseY, float partialTick);
        boolean handleClick(double mouseX, double mouseY);
        boolean handleScroll(int scrollAmount);
    }

    // ---- State ----

    private Tab currentTab = Tab.NEARBY;
    private Tab returnTab = Tab.NEARBY;

    private final net.minecraft.client.gui.components.Button[] pageButtons = new net.minecraft.client.gui.components.Button[4];

    final List<CommentEntry> commentList = new ArrayList<>();
    int latestCommentsRequestedAmount = 0;
    private static final int LATEST_PAGE_SIZE = 20;

    private double scrollCurrentPixel = 0;
    private double scrollTargetPixel = 0;
    private double scrollAnimStartPixel = 0;
    private long scrollAnimStartNanos = 0;
    private int[] snapPoints = new int[0];
    private int[] commentHeights = new int[0];
    private int totalContentHeight = 0;
    private int maxScrollPixel = 0;
    private boolean noMoreData = false;
    private int footerHeight = 0;

    private static final long SCROLL_ANIM_DURATION_NS = 200_000_000L;

    private static double easeOutCubic(double t) {
        double t1 = 1.0 - t;
        return 1.0 - t1 * t1 * t1;
    }

    private static double animateScroll(double startPixel, double targetPixel, long startNanos, long nowNanos) {
        if (startNanos == 0) return targetPixel;
        double t = (double) (nowNanos - startNanos) / SCROLL_ANIM_DURATION_NS;
        if (t >= 1.0) return targetPixel;
        return startPixel + (targetPixel - startPixel) * easeOutCubic(t);
    }

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

    private void recomputeSnapPoints() {
        if (height == 0 || listWidth <= 0) return;
        snapPoints = new int[commentList.size()];
        commentHeights = new int[commentList.size()];
        int y = 0;
        for (int i = 0; i < commentList.size(); i++) {
            snapPoints[i] = y;
            WidgetCommentEntry widget = getWidget(commentList.get(i));
            widget.showImage = true;
            widget.setBounds(xListL + 5, 0, listWidth - 10);
            commentHeights[i] = widget.getHeight() + 6;
            y += commentHeights[i];
        }
        totalContentHeight = y;
        footerHeight = 0;
        if (noMoreData && !commentList.isEmpty()) {
            footerHeight = 20 + (font != null ? font.lineHeight : 9) + 20;
            totalContentHeight += footerHeight;
        }
        int viewportHeight = height - 42;
        maxScrollPixel = Math.max(0, totalContentHeight - viewportHeight);
        scrollTargetPixel = Mth.clamp(scrollTargetPixel, 0, maxScrollPixel);
        scrollCurrentPixel = Mth.clamp(scrollCurrentPixel, 0, maxScrollPixel);
    }

    private int findSnapIndex(double pixelOffset) {
        for (int i = snapPoints.length - 1; i > 0; i--) {
            if (snapPoints[i] <= pixelOffset + 0.5) return i;
        }
        return 0;
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
            recomputeSnapPoints();
            return true;
        } else {
            commentToDelete = comment;
            return false;
        }
    }

    private static void renderIcon(ISnGuiCanvas g, int x, int y, int size, int u, int v, double mx, double my) {
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
        recomputeSnapPoints();

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
    public void extractRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiCanvas guiGraphics = ISnGuiCanvas.fromGuiParam(guiParam);

        Minecraft minecraft = Minecraft.getInstance();
        //? if <1.20.2
//extractBackground(guiParam);
        //? if >=1.21
        super.extractRenderState(guiParam, mouseX, mouseY, partialTick);
        guiGraphics.pushPose();
        guiGraphics.translate(0, 0, 1);

        int titleWidth = minecraft.font.width(Component.translatable("gui.worldcomment.list.title"));
        guiGraphics.text(minecraft.font, Component.translatable("gui.worldcomment.list.title"),
                xAsideLeftL + (ASIDE_L_WIDTH - titleWidth) / 2, 15, 0xFFFFE6C0, true);
        for (net.minecraft.client.gui.components.Button button : pageButtons) {
            //? if >=1.19.3 {
            button.setX(xAsideLeftL + 10);
            //?} else {
            /*button.x = xAsideLeftL + 10;
            *///?}
        }

        getCurrentView().render(guiParam, guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.popPose();
        //? if <1.21
//super.render(guiParam, mouseX, mouseY, partialTick);
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
    public void extractBackground(GuiGraphicsExtractor guiParam
                                  //? if >=1.20.2
                                  , int mouseX, int mouseY, float partialTick
    ) {
        ISnGuiCanvas guiGraphics = ISnGuiCanvas.fromGuiParam(guiParam);
        super.extractBackground(guiParam
            //? if >=1.20.2
            , mouseX, mouseY, partialTick
        );

        guiGraphics.enableBlend();

        guiParam.fill(xAsideLeftL, 0, xAsideLeftL + ASIDE_L_WIDTH, 70, 0x33e57373);
        guiParam.fillGradient(xAsideLeftL, 70, xAsideLeftL + ASIDE_L_WIDTH, 140, 0x33e57373, 0x00e57373);

        guiParam.fill(xListL - 2, 0, xListL - 1, height, 0x33FFFFFF);
        guiParam.fill(xListL - 1, 0, xListL, height, 0xBF000000);

        if (currentTab != Tab.DETAIL) {
            guiParam.fill(xAsideRightL - 2, 0, xAsideRightL - 1, height, 0xBF000000);
            guiParam.fill(xAsideRightL - 1, 0, xAsideRightL, height, 0x33FFFFFF);
        }

        guiGraphics.disableBlend();
    }

    @Override
    //? if >=1.20.2 {
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
    //?} else {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
    *///?}
        if (this.accumulatedScroll != 0.0 && Math.signum(deltaY) != Math.signum(this.accumulatedScroll)) {
            this.accumulatedScroll = 0.0;
        }
        this.accumulatedScroll += deltaY;
        int scrollAmount = (int)this.accumulatedScroll;
        //? if >=1.20.2 {
        if (scrollAmount == 0) return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        //?} else {
        /*if (scrollAmount == 0) return super.mouseScrolled(mouseX, mouseY, deltaY);
        *///?}
        this.accumulatedScroll -= scrollAmount;

        if (getCurrentView().handleScroll(scrollAmount)) {
            return true;
        }
        //? if >=1.20.2 {
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        //?} else {
        /*return super.mouseScrolled(mouseX, mouseY, deltaY);
        *///?}
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
        return true; // For the blurred background
    }

    // ---- Public API ----

    public void handleCommentDataUI(List<CommentEntry> data, long nonce, int rawCount) {
        if (nonce != lastRequestNonce) return;
        commentList.addAll(data);
        commentList.sort(Comparator.comparingLong(entry -> -entry.timestamp));
        if (currentTab == Tab.MY_POSTS || (currentTab == Tab.RECENT && rawCount < LATEST_PAGE_SIZE)) {
            noMoreData = true;
        }
        recomputeSnapPoints();
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

        CommentEntry targetEntry = ClientRayPicking.pickedComments.get(ClientRayPicking.overlayOffset);
//        if (targetEntry.initiator.equals(CommentEntry.SYSTEM_MESSAGE_MAGIC_INITIATOR)) return false;

        minecraft.execute(() -> {
            if (minecraft.screen instanceof CommentListScreen) {
                minecraft.screen.onClose();
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new CommentListScreen(targetEntry));
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
            scrollCurrentPixel = 0;
            scrollTargetPixel = 0;
            scrollAnimStartPixel = 0;
            scrollAnimStartNanos = 0;
            noMoreData = false;
            switch (currentTab) {
                case NEARBY -> {
                    BlockPos playerPos = minecraft.player.blockPosition();
                    for (Map<BlockPos, List<CommentEntry>> region : ClientWorldData.INSTANCE.regions.values()) {
                        for (Map.Entry<BlockPos, List<CommentEntry>> blockData : region.entrySet()) {
                            for (CommentEntry comment : blockData.getValue()) {
                                if (comment.deleted) continue;
                                if (comment.unlisted) continue;
                                commentList.add(comment);
                            }
                        }
                    }
                    commentList.sort(Comparator.comparingDouble(entry -> entry.location.distSqr(playerPos)));
                    noMoreData = true;
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
        public void render(GuiGraphicsExtractor guiParam,
                           ISnGuiCanvas guiGraphics, int mouseX, int mouseY, float partialTick) {
            scrollCurrentPixel = animateScroll(
                    scrollAnimStartPixel, scrollTargetPixel, scrollAnimStartNanos, System.nanoTime());

            int viewportTop = 20;
            int viewportBottom = height;

            guiParam.blit(RenderPipelines.GUI_TEXTURED, INWORLD_MENU_LIST_BACKGROUND,
                xListL, 0, 0, (int) scrollCurrentPixel,
                listWidth, height, 32, 32);

            guiGraphics.enableScissor(xListL, viewportTop, xListL + listWidth, viewportBottom);
            for (int i = 0; i < commentList.size(); i++) {
                int itemScreenY = viewportTop + snapPoints[i] - (int) scrollCurrentPixel;
                int itemBottom = itemScreenY + commentHeights[i] - 6;
                if (itemBottom <= viewportTop) continue;
                if (itemScreenY >= viewportBottom) break;

                CommentEntry comment = commentList.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                widget.showImage = true;
                widget.setBounds(xListL + 5, itemScreenY, listWidth - 10);
                widget.extractRenderState(guiParam, mouseX, mouseY, partialTick);
            }
            guiGraphics.disableScissor();

            for (int i = 0; i < commentList.size(); i++) {
                int itemScreenY = viewportTop + snapPoints[i] - (int) scrollCurrentPixel;
                int itemBottom = itemScreenY + commentHeights[i] - 6;
                if (itemBottom <= viewportTop) continue;
                if (itemScreenY >= viewportBottom) break;
                if (itemScreenY < viewportTop) continue;

                CommentEntry comment = commentList.get(i);
                int iconX = xAsideRightL + 4;
                renderIcon(guiGraphics, iconX, itemScreenY + 4, 16, 196, 60, mouseX, mouseY);

                if (canDelete(comment)) {
                    renderIcon(guiGraphics, iconX, itemScreenY + 4 + 16, 16, 216, 60, mouseX, mouseY);
                    if (hitTest(mouseX, mouseY, iconX, itemScreenY + 4 + 16, 16) && commentToDelete == comment) {
                        guiGraphics.renderTooltip(font, Component.translatable("gui.worldcomment.list.remove.confirm"), mouseX, mouseY);
                    }
                }
            }

            if (footerHeight > 0) {
                int footerTextY = viewportTop + (totalContentHeight - footerHeight) + 20 - (int) scrollCurrentPixel;
                if (footerTextY < viewportBottom && footerTextY + font.lineHeight > viewportTop) {
                    guiGraphics.centeredText(font,
                            Component.translatable("gui.worldcomment.list.no_more"),
                            xListL + listWidth / 2, footerTextY, 0xFFA5D6A7);
                }
            }

            if (commentList.size() > 1) {
                int topIndex = findSnapIndex(scrollCurrentPixel) + 1;
                String pageStr = String.format("↕ %d / %d", topIndex, commentList.size());
                guiGraphics.text(Minecraft.getInstance().font, pageStr,
                    xAsideRightL + 5, 5, 0xFFA5D6A7, true);
            } else if (commentList.isEmpty()) {
                guiGraphics.centeredText(Minecraft.getInstance().font,
                        Component.translatable("gui.worldcomment.list.empty"),
                        xListL + listWidth / 2, height / 2 - 5, 0xFFA5D6A7);
            }
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY) {
            int viewportTop = 20;
            int viewportBottom = height;

            for (int i = 0; i < commentList.size(); i++) {
                int itemScreenY = viewportTop + snapPoints[i] - (int) scrollCurrentPixel;
                int itemBottom = itemScreenY + commentHeights[i] - 6;
                if (itemBottom <= viewportTop) continue;
                if (itemScreenY >= viewportBottom) break;
                if (itemScreenY < viewportTop) continue;

                CommentEntry comment = commentList.get(i);
                int iconX = xAsideRightL + 5;

                if (hitTest(mouseX, mouseY, iconX, itemScreenY + 4, 16)) {
                    commentForDetail = comment;
                    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                    switchTo(Tab.DETAIL);
                    return true;
                }

                if (canDelete(comment) && hitTest(mouseX, mouseY, iconX, itemScreenY + 4 + 16, 16)) {
                    AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                    tryDelete(comment);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleScroll(int scrollAmount) {
            if (commentList.isEmpty()) return false;
            int dir = -(int) Math.signum(scrollAmount);
            int lastSnap = snapPoints.length > 0 ? snapPoints[snapPoints.length - 1] : 0;
            boolean pastLastSnap = scrollTargetPixel > lastSnap + 0.5;

            double newTarget;
            if (dir < 0 && pastLastSnap) {
                newTarget = lastSnap;
            } else if (dir > 0 && !pastLastSnap
                    && findSnapIndex(scrollTargetPixel) == commentList.size() - 1
                    && footerHeight > 0) {
                newTarget = maxScrollPixel;
            } else {
                int currentSnapIndex = findSnapIndex(scrollTargetPixel);
                int newIndex = Mth.clamp(currentSnapIndex + dir, 0, commentList.size() - 1);
                newTarget = Mth.clamp((double) snapPoints[newIndex], 0, maxScrollPixel);
            }
            scrollAnimStartPixel = scrollCurrentPixel;
            scrollAnimStartNanos = System.nanoTime();
            scrollTargetPixel = newTarget;

            int visibleIndex = findSnapIndex(scrollTargetPixel);
            if (currentTab == Tab.RECENT && !noMoreData
                    && visibleIndex >= latestCommentsRequestedAmount - LATEST_PAGE_SIZE / 2) {
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

        private double detailScrollCurrent = 0;
        private double detailScrollTarget = 0;
        private double detailScrollAnimStart = 0;
        private long detailScrollAnimStartNanos = 0;
        private int detailContentHeight = 0;
        private int detailMaxScroll = 0;

        private int cachedImgX, cachedImgY, cachedImgW, cachedImgH;
        private boolean hasImage = false;
        private int cachedDeleteBtnX, cachedDeleteBtnY;
        private boolean hasDeleteBtn = false;

        private static final int CARD_MARGIN = 8;
        private static final int CARD_PADDING = 10;
        private static final int CARD_GAP = 10;
        private static final int HEADER_H = 22;
        private static final int SHADOW_OFFSET = 2;

        private static final int COLOR_CARD_BODY = 0xAA2d2d3d;
        private static final int COLOR_CARD_HEADER = 0xBB222233;
        private static final int COLOR_SHADOW = 0x66000000;
        private static final int COLOR_LABEL = 0xFFAAAAAA;
        private static final int COLOR_VALUE = 0xFFFFFFFF;
        private static final int COLOR_UUID = 0xFF888888;
        private static final int COLOR_SECONDARY = 0xFFBBBBBB;
        private static final int COLOR_HINT = 0xFF8888FF;

        private void drawCard(ISnGuiCanvas g, int x, int y, int w, int h, boolean hasHeader) {
            g.fill(x + SHADOW_OFFSET, y + SHADOW_OFFSET,
                    x + w + SHADOW_OFFSET, y + h + SHADOW_OFFSET, COLOR_SHADOW);
            if (hasHeader) {
                g.fill(x, y + HEADER_H, x + w, y + h, COLOR_CARD_BODY);
                g.fill(x, y, x + w, y + HEADER_H, COLOR_CARD_HEADER);
            } else {
                g.fill(x, y, x + w, y + h, COLOR_CARD_BODY);
            }
        }

        @Override
        public void onEnter() {
            detailScrollCurrent = 0;
            detailScrollTarget = 0;
            detailScrollAnimStart = 0;
            detailScrollAnimStartNanos = 0;
        }

        @Override
        public void render(GuiGraphicsExtractor guiParam,
                           ISnGuiCanvas guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            CommentEntry comment = commentForDetail;
            if (comment == null) return;

            int dLeft = xListL;
            int dWidth = (xAsideRightL + ASIDE_R_WIDTH) - xListL;

            int cardX = dLeft + CARD_MARGIN;
            int cardW = dWidth - 2 * CARD_MARGIN;
            int contentX = cardX + CARD_PADDING;
            int contentW = cardW - 2 * CARD_PADDING;

            detailScrollCurrent = animateScroll(
                    detailScrollAnimStart, detailScrollTarget,
                    detailScrollAnimStartNanos, System.nanoTime());

            int viewportTop = 10;
            int viewportBottom = height;
            int viewportHeight = viewportBottom - viewportTop;

            guiParam.blit(RenderPipelines.GUI_TEXTURED, INWORLD_MENU_LIST_BACKGROUND,
                xListL, 0, 0, (int) scrollCurrentPixel,
                dWidth, height, 32, 32);

            guiGraphics.enableScissor(dLeft, viewportTop, dLeft + dWidth, viewportBottom);

            int y = viewportTop + CARD_MARGIN - (int) detailScrollCurrent;
            int startY = y;

            // ======== Card 1: Metadata ========
            boolean isAdmin = minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
            int metaRows = isAdmin ? 4 : 3;
            int metaBodyH = CARD_PADDING + (metaRows - 1) * (font.lineHeight + 4) + font.lineHeight + CARD_PADDING;
            int card1H = HEADER_H + metaBodyH;

            drawCard(guiGraphics, cardX, y, cardW, card1H, true);

            // Header: type icon + type name
            TextureAtlasSprite iconSprite = EmojiRegistry.INSTANCE.getSprite(comment.messageType);
            guiGraphics.enableBlend();
            guiParam.pose().pushMatrix();
            guiParam.pose().translate(0.5f, 0.5f);
            guiParam.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite,
                    contentX, y + (HEADER_H - 14) / 2, 14, 14);
            guiParam.pose().popMatrix();
            guiGraphics.disableBlend();

            Component typeName = Component.translatable("gui.worldcomment.comment_type." + comment.messageType)
                    .setStyle(Style.EMPTY.withBold(true));
            guiGraphics.text(font, typeName, contentX + 18, y + (HEADER_H - font.lineHeight) / 2, COLOR_VALUE, true);

            // Header: delete button (right side)
            hasDeleteBtn = false;
            if (canDelete(comment)) {
                cachedDeleteBtnX = cardX + cardW - CARD_PADDING - 16;
                cachedDeleteBtnY = y + (HEADER_H - 16) / 2;
                hasDeleteBtn = true;
                renderIcon(guiGraphics, cachedDeleteBtnX, cachedDeleteBtnY, 16, 216, 60, mouseX, mouseY);
                if (hitTest(mouseX, mouseY, cachedDeleteBtnX, cachedDeleteBtnY, 16) && commentToDelete == comment) {
                    guiGraphics.renderTooltip(font,
                            Component.translatable("gui.worldcomment.list.remove.confirm"), mouseX, mouseY);
                }
            }

            // Body: two-column metadata
            int labelColW = Math.max(
                    Math.max(font.width(Component.translatable("gui.worldcomment.detail.label.author")),
                             font.width(Component.translatable("gui.worldcomment.detail.label.time"))),
                    Math.max(font.width(Component.translatable("gui.worldcomment.detail.label.location")),
                             font.width("UUID"))
            );
            int valueX = contentX + labelColW + 6;
            int rowY = y + HEADER_H + CARD_PADDING;

            Component nameComponent = comment.initiatorName.isEmpty()
                    ? Component.translatable("gui.worldcomment.anonymous")
                    : Component.literal(comment.initiatorName);
            guiGraphics.text(font, Component.translatable("gui.worldcomment.detail.label.author"),
                    contentX, rowY, COLOR_LABEL, true);
            guiGraphics.text(font, nameComponent, valueX, rowY, COLOR_VALUE, true);
            rowY += font.lineHeight + 4;

            if (isAdmin) {
                guiGraphics.text(font, "UUID", contentX, rowY, COLOR_LABEL, true);
                guiGraphics.text(font, comment.initiator.toString(), valueX, rowY, COLOR_UUID, true);
                rowY += font.lineHeight + 4;
            }

            String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                    .format(Instant.ofEpochMilli(comment.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
            guiGraphics.text(font, Component.translatable("gui.worldcomment.detail.label.time"),
                    contentX, rowY, COLOR_LABEL, true);
            guiGraphics.text(font, timeStr, valueX, rowY, COLOR_SECONDARY, true);
            rowY += font.lineHeight + 4;

            String locStr = comment.level.toString() + "  " + comment.location.toShortString();
            guiGraphics.text(font, Component.translatable("gui.worldcomment.detail.label.location"),
                    contentX, rowY, COLOR_LABEL, true);
            guiGraphics.text(font, locStr, valueX, rowY, COLOR_SECONDARY, true);

            y += card1H + CARD_GAP;

            // ======== Card 2: Text (hidden if empty) ========
            if (!comment.message.isEmpty()) {
                List<SizedFormattedText> lines = SizedFormattedText.splitLines(comment.message, font, contentW, Style.EMPTY,
                    CommentEntry.isMarkerType(comment.messageType), true);
                int textH = 0;
                for (SizedFormattedText line : lines) {
                    textH += (int)(font.lineHeight * line.sizeModifier) + 1;
                }
                int card2H = CARD_PADDING + textH + CARD_PADDING;

                drawCard(guiGraphics, cardX, y, cardW, card2H, false);

                int textY = y + CARD_PADDING;
                for (SizedFormattedText line : lines) {
                    guiGraphics.pushPose();
                    guiGraphics.translate(contentX, textY, 0);
                    guiGraphics.scale(line.sizeModifier, line.sizeModifier);
                    guiGraphics.text(font, line.ordered, 0, 0, 0xFFDDDDDD, true);
                    guiGraphics.popPose();
                    textY += (int)(font.lineHeight * line.sizeModifier) + 1;
                }

                y += card2H + CARD_GAP;
            }

            // ======== Card 3: Image (hidden if no image) ========
            hasImage = false;
            if (!comment.image.sourceUrl.isEmpty()) {
                String detailUrl = ImageUrlResolver.resolve(comment.image, ImageUrlResolver.ImageUsagePurpose.DETAIL,
                        MainClient.CLIENT_CONFIG.serverIssuedConfig.imageVariants,
                        MainClient.CLIENT_CONFIG.serverIssuedConfig.uploaderCdnConfigs);
                ImageDownload.ImageState imageState = ImageDownload.getTexture(detailUrl);
                int maxImgW = contentW;
                int maxImgH = viewportHeight / 2;
                int imgW, imgH;
                if (imageState.width * maxImgH > imageState.height * maxImgW) {
                    imgW = maxImgW;
                    imgH = Math.max(1, maxImgW * imageState.height / imageState.width);
                } else {
                    imgH = maxImgH;
                    imgW = Math.max(1, maxImgH * imageState.width / imageState.height);
                }

                int footerH = font.lineHeight + 4;
                int card3H = CARD_PADDING + imgH + 4 + footerH + CARD_PADDING;

                drawCard(guiGraphics, cardX, y, cardW, card3H, false);

                int imgX = contentX + (contentW - imgW) / 2;
                int imgY = y + CARD_PADDING;

                guiGraphics.blit(imageState.getFriendlyTexture(minecraft.getTextureManager()),
                        imgX, imgY, imgX + imgW, imgY + imgH);

                cachedImgX = imgX;
                cachedImgY = imgY;
                cachedImgW = imgW;
                cachedImgH = imgH;
                hasImage = true;

                Component hint = Component.translatable("gui.worldcomment.detail.click_to_view");
                int hintWidth = font.width(hint);
                guiGraphics.text(font, hint,
                        contentX + (contentW - hintWidth) / 2,
                        imgY + imgH + 4, COLOR_HINT, true);

                y += card3H + CARD_GAP;
            }

            y += CARD_MARGIN;

            guiGraphics.disableScissor();

            detailContentHeight = y - startY;
            detailMaxScroll = Math.max(0, detailContentHeight - viewportHeight);
            detailScrollTarget = Mth.clamp(detailScrollTarget, 0, detailMaxScroll);
            detailScrollCurrent = Mth.clamp(detailScrollCurrent, 0, detailMaxScroll);
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY) {
            CommentEntry comment = commentForDetail;
            if (comment == null) return false;

            if (hasImage && mouseX >= cachedImgX && mouseX < cachedImgX + cachedImgW
                    && mouseY >= cachedImgY && mouseY < cachedImgY + cachedImgH) {
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                Minecraft.getInstance().setScreen(
                        new ImageViewScreen(CommentListScreen.this, comment.image));
                return true;
            }

            if (hasDeleteBtn && hitTest(mouseX, mouseY, cachedDeleteBtnX, cachedDeleteBtnY, 16)) {
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                if (tryDelete(comment)) {
                    onClose();
                }
                return true;
            }

            return false;
        }

        @Override
        public boolean handleScroll(int scrollAmount) {
            if (detailMaxScroll <= 0) return false;
            detailScrollAnimStart = detailScrollCurrent;
            detailScrollAnimStartNanos = System.nanoTime();
            detailScrollTarget = Mth.clamp(detailScrollTarget - scrollAmount * 20, 0, detailMaxScroll);
            return true;
        }
    }
}
