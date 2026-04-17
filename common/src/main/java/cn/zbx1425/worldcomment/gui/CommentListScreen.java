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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.locale.Language;
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

        if (currentTab != Tab.DETAIL) {
            guiParam.fill(xAsideRightL - 2, 0, xAsideRightL - 1, height, 0xBF000000);
            guiParam.fill(xAsideRightL - 1, 0, xAsideRightL, height, 0x33FFFFFF);
        }

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
        public void render(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam,
                           ISnGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
                int iconX = xAsideRightL + 5;
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
                    guiGraphics.drawCenteredString(font,
                            Component.translatable("gui.worldcomment.list.no_more"),
                            xListL + listWidth / 2, footerTextY, 0xFFA5D6A7);
                }
            }

            if (commentList.size() > 1) {
                int topIndex = findSnapIndex(scrollCurrentPixel) + 1;
                String pageStr = String.format("↕ %d / %d", topIndex, commentList.size());
                guiGraphics.drawString(Minecraft.getInstance().font, pageStr,
                    xAsideRightL + 5, 5, 0xFFA5D6A7, true);
            } else if (commentList.isEmpty()) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font,
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
                    switchTo(Tab.DETAIL);
                    return true;
                }

                if (canDelete(comment) && hitTest(mouseX, mouseY, iconX, itemScreenY + 4 + 16, 16)) {
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

        private static final int PADDING = 12;

        @Override
        public void onEnter() {
            detailScrollCurrent = 0;
            detailScrollTarget = 0;
            detailScrollAnimStart = 0;
            detailScrollAnimStartNanos = 0;
        }

        @Override
        public void render(#if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiParam,
                           ISnGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            CommentEntry comment = commentForDetail;
            if (comment == null) return;

            int dLeft = xListL;
            int dWidth = (xAsideRightL + ASIDE_R_WIDTH) - xListL;
            int contentWidth = dWidth - 2 * PADDING;
            int contentLeft = dLeft + PADDING;

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

            int y = viewportTop - (int) detailScrollCurrent;
            int startY = y;

            // --- Metadata ---
            TextureAtlasSprite iconSprite = EmojiRegistry.INSTANCE.getSprite(comment.messageType);
            guiGraphics.enableBlend();
            guiParam.pose().pushMatrix();
            guiParam.pose().translate(0.5f, 0.5f);
            guiParam.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, contentLeft, y + 1, 14, 14);
            guiParam.pose().popMatrix();
            guiGraphics.disableBlend();
            Component typeName = Component.translatable("gui.worldcomment.comment_type." + comment.messageType)
                    .setStyle(Style.EMPTY.withBold(true).withColor(
                            CommentTypeButton.COMMENT_TYPE_COLOR[comment.messageType - 1] & 0xFFFFFF));
            guiGraphics.drawString(font, typeName, contentLeft + 18, y + 3, 0xFFFFFFFF, true);
            y += 25;

            Component nameComponent = comment.initiatorName.isEmpty()
                    ? Component.translatable("gui.worldcomment.anonymous")
                    : Component.literal(comment.initiatorName);
            guiGraphics.drawString(font, nameComponent, contentLeft, y, 0xFFFFFFFF, true);
            if (minecraft.player.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) {
                y += 14;
                String uuid = comment.initiator.toString();
                guiGraphics.drawString(font, uuid,
                        contentLeft, y, 0xFF888888, true);
            }
            y += 14;

            String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                    .format(Instant.ofEpochMilli(comment.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());
            guiGraphics.drawString(font, timeStr, contentLeft, y, 0xFFBBBBBB, true);
            y += 14;

            String locStr = comment.level.toString() + "  " + comment.location.toShortString();
            guiGraphics.drawString(font, locStr, contentLeft, y, 0xFFBBBBBB, true);
            y += 14;

            y += 6;
            guiGraphics.fill(contentLeft, y, contentLeft + contentWidth, y + 1, 0x44FFFFFF);
            y += 7;

            // --- Comment text ---
            if (!comment.message.isEmpty()) {
                List<FormattedCharSequence> lines = Language.getInstance().getVisualOrder(
                        font.getSplitter().splitLines(comment.message, contentWidth, Style.EMPTY));
                for (FormattedCharSequence line : lines) {
                    guiGraphics.drawString(font, line, contentLeft, y, 0xFFEEEEEE, true);
                    y += font.lineHeight + 1;
                }
                y += 6;
            }

            // --- Image ---
            hasImage = false;
            if (!comment.image.url.isEmpty()) {
                ImageDownload.ImageState imageState = ImageDownload.getTexture(comment.image, false);
                int maxImgW = (int) (contentWidth * 0.8);
                int maxImgH = height / 2;
                int imgW, imgH;
                if (imageState.width * maxImgH > imageState.height * maxImgW) {
                    imgW = maxImgW;
                    imgH = Math.max(1, maxImgW * imageState.height / imageState.width);
                } else {
                    imgH = maxImgH;
                    imgW = Math.max(1, maxImgH * imageState.width / imageState.height);
                }
                int imgX = contentLeft + (contentWidth - imgW) / 2;

                guiGraphics.fill(imgX + 2, y + 2, imgX + imgW + 2, y + imgH + 2, 0xFF000000);
                guiGraphics.blit(imageState.getFriendlyTexture(minecraft.getTextureManager()),
                        imgX, y, imgX + imgW, y + imgH);

                cachedImgX = imgX;
                cachedImgY = y;
                cachedImgW = imgW;
                cachedImgH = imgH;
                hasImage = true;

                y += imgH + 4;

                Component hint = Component.translatable("gui.worldcomment.detail.click_to_view");
                int hintWidth = font.width(hint);
                guiGraphics.drawString(font, hint,
                        contentLeft + (contentWidth - hintWidth) / 2, y, 0xFF8888FF, true);
                y += font.lineHeight + 4;
            }

            // --- Delete button ---
            hasDeleteBtn = false;
            if (canDelete(comment)) {
                y += 4;
                cachedDeleteBtnX = contentLeft;
                cachedDeleteBtnY = y;
                hasDeleteBtn = true;
                renderIcon(guiGraphics, contentLeft, y, 20, 216, 60, mouseX, mouseY);
                if (hitTest(mouseX, mouseY, contentLeft, y, 20) && commentToDelete == comment) {
                    guiGraphics.renderTooltip(font,
                            Component.translatable("gui.worldcomment.list.remove.confirm"), mouseX, mouseY);
                }
                y += 24;
            }

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
                Minecraft.getInstance().setScreen(
                        new ImageViewScreen(CommentListScreen.this, comment.image));
                return true;
            }

            if (hasDeleteBtn && hitTest(mouseX, mouseY, cachedDeleteBtnX, cachedDeleteBtnY, 20)) {
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
