package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private long timestampOpenGui = 0L;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        long timestampNow = System.currentTimeMillis();
        if (timestampOpenGui == 0) timestampOpenGui = timestampNow;

        float animProgress = Mth.clamp((timestampNow - timestampOpenGui) / 300f, 0f, 1f);
        float finalWidth = (width / 3f - SQ_SIZE * 2);
        float finalHeight = finalWidth / width * height;
        float x1 = Mth.lerp(animProgress, 0, width - SQ_SIZE - finalWidth);
        float x2 = Mth.lerp(animProgress, width, width - SQ_SIZE);
        float y1 = Mth.lerp(animProgress, 0, SQ_SIZE);
        float y2 = Mth.lerp(animProgress, height, SQ_SIZE + finalHeight);

        int shadowColor = 0xAA222222;
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
    }
}
