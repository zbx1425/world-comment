package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientDatabase;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.*;

public class CommentListScreen extends Screen implements IGuiCommon {

    protected CommentListScreen(CommentEntry commentForDetail) {
        super(Component.literal(""));
        this.commentForDetail = commentForDetail;
        useSubScreen(commentForDetail != null ? 3 : 0);
        prevSubScreen = commentForDetail != null ? 3 : 0;
    }

    int subScreen = 0, prevSubScreen = 0;

    Button[] pageButtons = new Button[4];

    List<CommentEntry> commentList = new ArrayList<>();
    int commentListOffset = 0;

    CommentEntry commentForDetail;

    private final Map<CommentEntry, WidgetCommentEntry> widgets = new WeakHashMap<>();

    private WidgetCommentEntry getWidget(CommentEntry entry) {
        return widgets.computeIfAbsent(entry, WidgetCommentEntry::new);
    }

    @Override
    protected void init() {
        super.init();
        pageButtons[0] = addRenderableWidget(Button.builder(Component.translatable("gui.worldcomment.list.nearby_posts"), sender -> useSubScreen(0))
                .bounds(10, 40, 80, 20).build());
        pageButtons[1] = addRenderableWidget(Button.builder(Component.translatable("gui.worldcomment.list.recent_posts"), sender -> useSubScreen(1))
                .bounds(10, 64, 80, 20).build());
        pageButtons[2] = addRenderableWidget(Button.builder(Component.translatable("gui.worldcomment.list.my_posts"), sender -> useSubScreen(2))
                .bounds(10, 88, 80, 20).build());
        pageButtons[3] = addRenderableWidget(Button.builder(Component.translatable("gui.worldcomment.list.detail"), sender -> {})
                .bounds(10, 122, 80, 20).build());
        for (int i = 0; i < pageButtons.length; i++) {
            pageButtons[i].active = i != subScreen;
        }
        pageButtons[3].visible = subScreen == 3;
    }

    void useSubScreen(int subScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        this.prevSubScreen = this.subScreen;
        this.subScreen = subScreen;
        switch (subScreen) {
            case 0:
                commentList.clear();
                BlockPos playerPos = minecraft.player.blockPosition();
                for (Map<BlockPos, List<CommentEntry>> region : ClientDatabase.INSTANCE.regions.values()) {
                    for (Map.Entry<BlockPos, List<CommentEntry>> blockData : region.entrySet()) {
                        commentList.addAll(blockData.getValue());
                    }
                }
                commentList.sort(Comparator.comparingDouble(entry -> entry.location.distSqr(playerPos)));
                commentListOffset = prevSubScreen == 3
                        ? Mth.clamp(0, commentListOffset, Math.max(commentList.size() - 1, 0)) : 0;
                break;
            case 1:
                commentList.clear();
                commentListOffset = prevSubScreen == 3
                        ? Mth.clamp(0, commentListOffset, Math.max(commentList.size() - 1, 0)) : 0;
                break;
            case 2:
                commentList.clear();
                commentListOffset = prevSubScreen == 3
                        ? Mth.clamp(0, commentListOffset, Math.max(commentList.size() - 1, 0)) : 0;
                break;
            case 3:

                break;
        }
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        renderBackground(guiGraphics);
        guiGraphics.drawString(minecraft.font, Component.translatable("gui.worldcomment.list.title"),
                40, 15, 0xFFFFE6C0, true);

        if (subScreen == 3) {
            CommentEntry comment = commentForDetail;
            if (!comment.image.url.isEmpty()) {
                int picWidth = width - 100 - 20 - 20;
                int picHeight = picWidth * 9 / 16;
                int x1 = 100 + 10, x2 = 100 + 10 + picWidth;
                int y1 = 30 + 10, y2 = 30 + 10 + picHeight;

                int shadowColor = 0xFF000000;
                int shadowOffset = 3;
                guiGraphics.fill(
                        (int) (x1 + shadowOffset), (int) (y1 + shadowOffset),
                        (int) (x2 + shadowOffset), (int) (y2 + shadowOffset),
                        shadowColor
                );
                RenderSystem.setShaderTexture(0, ImageDownload.getTexture(comment.image.url).getId());
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                Matrix4f matrix4f = guiGraphics.pose().last().pose();
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(0, 0).endVertex();
                bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(0, 1).endVertex();
                bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(1, 1).endVertex();
                bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(1, 0).endVertex();
                BufferUploader.drawWithShader(bufferBuilder.end());
            }
        } else {
            graphicsBlit9(guiGraphics, 100, 30, width - 120, height - 50,
                    176, 40, 20, 20, 256, 256,
                    4, 4, 4, 4
            );
            guiGraphics.enableScissor(102, 32, width - 22, height - 22);
            int yOffset = 32 + 4;
            for (int i = commentListOffset; i < commentList.size(); i++) {
                CommentEntry comment = commentList.get(i);
                WidgetCommentEntry widget = getWidget(comment);
                widget.setBounds(106, yOffset, width - 102 - 22 - 8 - 4 - 16);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);

                guiGraphics.blit(ATLAS_LOCATION, width - 22 - 4 - 16, yOffset + 4, 16, 16,
                        196, 60, 20, 20, 256, 256);
                if (mouseX > width - 22 - 4 - 16 && mouseX < width - 22 - 4
                        && mouseY > yOffset + 4 && mouseY < yOffset + 4 + 16) {
                    guiGraphics.blit(ATLAS_LOCATION, width - 22 - 4 - 16, yOffset + 4, 16, 16,
                            236, 60, 20, 20, 256, 256);
                }

                boolean canDelete = minecraft.player.hasPermissions(3)
                        || minecraft.player.getGameProfile().getId().equals(comment.initiator);
                if (canDelete) {
                    yOffset += 16;
                    guiGraphics.blit(ATLAS_LOCATION, width - 22 - 4 - 16, yOffset + 4, 16, 16,
                            216, 60, 20, 20, 256, 256);
                    if (mouseX > width - 22 - 4 - 16 && mouseX < width - 22 - 4
                            && mouseY > yOffset + 4 && mouseY < yOffset + 4 + 16) {
                        guiGraphics.blit(ATLAS_LOCATION, width - 22 - 4 - 16, yOffset + 4, 16, 16,
                                236, 60, 20, 20, 256, 256);
                    }
                    yOffset -= 16;
                }

                yOffset += widget.getHeight() + 6;
                if (yOffset > height - 22) break;
            }
            guiGraphics.disableScissor();

            String pageStr = String.format("↕ %d / %d", commentListOffset + 1, commentList.size());
            guiGraphics.drawString(Minecraft.getInstance().font, pageStr,
                    width - 10 - 10 - Minecraft.getInstance().font.width(pageStr),
                    15, 0xFFA5D6A7, true);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (subScreen == 3) return super.mouseClicked(mouseX, mouseY, button);

        int yOffset = 32 + 4;
        for (int i = commentListOffset; i < commentList.size(); i++) {
            CommentEntry comment = commentList.get(i);
            WidgetCommentEntry widget = getWidget(comment);

            if (mouseX > width - 22 - 4 - 16 && mouseX < width - 22 - 4
                    && mouseY > yOffset + 4 && mouseY < yOffset + 4 + 16) {
                this.commentForDetail = comment;
                useSubScreen(3);
                return true;
            }

            yOffset += 16;
            if (mouseX > width - 22 - 4 - 16 && mouseX < width - 22 - 4
                    && mouseY > yOffset + 4 && mouseY < yOffset + 4 + 16) {
                this.commentForDetail = comment;
                useSubScreen(3);
                return true;
            }
            yOffset -= 16;

            yOffset += widget.getHeight() + 6;
            if (yOffset > height - 22) break;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        graphicsBlit9(guiGraphics, 30, 10, width - 40, height - 20,
                196, 40, 20, 20, 256, 256,
                4, 4, 4, 4
        );
    }

    private double accumulatedScroll = 0;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.accumulatedScroll != 0.0 && Math.signum(delta) != Math.signum(this.accumulatedScroll)) {
            this.accumulatedScroll = 0.0;
        }
        this.accumulatedScroll += delta;
        int scrollAmount = (int)this.accumulatedScroll;
        if (scrollAmount == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        this.accumulatedScroll -= scrollAmount;
        if (commentList.size() > 1) {
            int dir = -(int)Math.signum(scrollAmount);
            commentListOffset = Mth.clamp(commentListOffset + dir, 0, commentList.size() - 1);
            return true;
        } else {
            commentListOffset = 0;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (subScreen == 3 && prevSubScreen != 3) {
            useSubScreen(prevSubScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static boolean handleKeyF5() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        ItemStack item = minecraft.player.getMainHandItem();
        if (!item.is(Main.ITEM_COMMENT_TOOL.get())) return false;

        minecraft.execute(() -> {
            if (minecraft.screen instanceof CommentListScreen) {
                minecraft.screen.onClose();
            } else {
                minecraft.setScreen(new CommentListScreen(null));
            }
        });
        return true;
    }

    public static boolean handleKeyTab() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        if (ClientRayPicking.pickedComments.size() == 0) return false;

        minecraft.execute(() -> {
            if (minecraft.screen instanceof CommentListScreen) {
                minecraft.screen.onClose();
            } else {
                minecraft.setScreen(
                        new CommentListScreen(ClientRayPicking.pickedComments.get(ClientRayPicking.overlayOffset)));
            }
        });
        return true;
    }
}
