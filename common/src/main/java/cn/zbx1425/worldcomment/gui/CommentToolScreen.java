package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CommentToolScreen extends Screen {

    private Path imagePath;

    private final DynamicTexture glTexture;

    private static final int SQ_SIZE = 20;

    public CommentToolScreen(Path imagePath) {
        super(Component.literal("Comment Tool"));
        try (FileInputStream fis = new FileInputStream(imagePath.toFile())) {
            glTexture = new DynamicTexture(NativeImage.read(fis));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onClose() {
        glTexture.close();
        super.onClose();
    }

    private List<CommentTypeButton> radioButtons = new ArrayList<>();
    private MultiLineEditBox textBoxMessage;
    private Checkbox checkBoxIncludeImage;
    private Button btnSendFeedback;
    private int selectedCommentType = 0;

    @Override
    protected void init() {
        super.init();

        int baseY = SQ_SIZE;

        radioButtons.clear();
        for (int r = 0; r < 2; r++) {
            addRenderableWidget(new WidgetLabel(
                    SQ_SIZE, baseY,
                    SQ_SIZE * 4, Component.translatable("gui.worldcomment.comment_type.r" + (r + 1))
            ));
            for (int c = 0; c < 4; c++) {
                CommentTypeButton selectBtn = new CommentTypeButton(
                    SQ_SIZE + CommentTypeButton.BTN_WIDTH * c,
                    baseY + SQ_SIZE / 2,
                    r * 4 + c + 1, sender -> {
                        selectedCommentType = ((CommentTypeButton)sender).commentType;
                        for (CommentTypeButton radioButton : radioButtons) {
                            radioButton.active = radioButton.commentType != selectedCommentType;
                        }
                        btnSendFeedback.active = selectedCommentType != 0;
                    }
                );
                selectBtn.active = selectBtn.commentType != selectedCommentType;
                addRenderableWidget(selectBtn);
                radioButtons.add(selectBtn);
            }
            baseY += CommentTypeButton.BTN_HEIGHT + SQ_SIZE / 2;
        }
        baseY += SQ_SIZE / 2;

        addRenderableWidget(new WidgetLabel(
                SQ_SIZE, baseY,
                CommentTypeButton.BTN_WIDTH * 4, Component.translatable("gui.worldcomment.message")
        ));
        baseY += SQ_SIZE / 2;
        textBoxMessage = new MultiLineEditBox(
                Minecraft.getInstance().font,
                SQ_SIZE, baseY, CommentTypeButton.BTN_WIDTH * 4, SQ_SIZE * 3,
                Component.translatable("gui.worldcomment.message.placeholder"),
                Component.literal("")
        );
        addRenderableWidget(textBoxMessage);
        baseY += textBoxMessage.getHeight();
        baseY += SQ_SIZE / 2;

        btnSendFeedback = Button.builder(Component.translatable("gui.worldcomment.submit"),
                sender -> sendReport()).pos(SQ_SIZE, baseY).width(CommentTypeButton.BTN_WIDTH * 4).build();
        btnSendFeedback.active = selectedCommentType != 0;
        addRenderableWidget(btnSendFeedback);


        float imgWidth = (width / 3f - SQ_SIZE * 2);
        float imgHeight = imgWidth / glTexture.getPixels().getWidth() * glTexture.getPixels().getHeight();
        baseY = (int) (SQ_SIZE + imgHeight + SQ_SIZE / 2);
        checkBoxIncludeImage = new Checkbox(
                width - (int)imgWidth - SQ_SIZE, baseY, (int)imgWidth, SQ_SIZE,
                Component.translatable("gui.worldcomment.include_screenshot"),
                true, true
        );
        addRenderableWidget(checkBoxIncludeImage);
    }

    private void sendReport() {
        if (selectedCommentType == 0) return;
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("TODO ..."), true);
        });
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        boolean animationCompleted = renderImageAnimation(guiGraphics);
        if (animationCompleted) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private long timestampOpenGui = 0L;

    private boolean renderImageAnimation(GuiGraphics guiGraphics) {
        long timestampNow = System.currentTimeMillis();
        if (timestampOpenGui == 0) timestampOpenGui = timestampNow;

        float animProgress = Mth.clamp((timestampNow - timestampOpenGui) / 300f, 0f, 1f);
        float finalWidth = (width / 3f - SQ_SIZE * 2);
        float finalHeight = finalWidth / glTexture.getPixels().getWidth() * glTexture.getPixels().getHeight();
        float x1 = Mth.lerp(animProgress, 0, width - SQ_SIZE - finalWidth);
        float x2 = Mth.lerp(animProgress, width, width - SQ_SIZE);
        float y1 = Mth.lerp(animProgress, 0, SQ_SIZE);
        float y2 = Mth.lerp(animProgress, height, SQ_SIZE + finalHeight);

        int shadowColor = 0xCC222222;
        int shadowOffset = 2;
        guiGraphics.fill(
                (int) (x1 + shadowOffset), (int) (y1 + shadowOffset),
                (int) (x2 + shadowOffset), (int) (y2 + shadowOffset),
                shadowColor
        );

        RenderSystem.setShaderTexture(0, glTexture.getId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(0, 0).endVertex();
        bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(0, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(1, 1).endVertex();
        bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        return animProgress >= 1f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
