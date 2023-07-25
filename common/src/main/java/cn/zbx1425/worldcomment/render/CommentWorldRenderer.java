package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

import java.util.Random;

public class CommentWorldRenderer {

    private static final ResourceLocation ATLAS_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/comment-tool.png");

    private static final Random RANDOM = new Random();

    public static void renderComment(VertexConsumer vertices, PoseStack matrices, CommentEntry comment, int light, boolean jumpy) {
        RANDOM.setSeed(comment.id);
        matrices.pushPose();
        matrices.translate(comment.location.getX() + 0.5f, comment.location.getY(),
                comment.location.getZ() + 0.5f);
        float cycleLength = jumpy ? 1000 : 2000;
        float cycleX = ((System.currentTimeMillis() + RANDOM.nextLong(0, (long)cycleLength)) % (long)cycleLength) / cycleLength;
        float cycleY = jumpy ? (-4 * cycleX * (cycleX - 1)) : (float)Math.sin(cycleX * Math.PI * 2) / 2 + 0.5f;
        matrices.translate(
                RANDOM.nextFloat(-0.3f, 0.3f),
                cycleY * 0.6,
                RANDOM.nextFloat(-0.3f, 0.3f)
        );
        matrices.mulPose(Axis.YP.rotation(cycleX * Mth.PI * 2 + RANDOM.nextFloat(0, Mth.PI * 2)));
        matrices.scale(0.4f, 0.4f, 0.4f);
        float u1 = ((comment.messageType - 1) % 4) * 0.25f;
        float v1 = (int)((comment.messageType - 1) / 4) * 0.25f + 0.5f;
        float u2 = u1 + 0.25f, v2 = v1 + 0.25f;
        PoseStack.Pose pose = matrices.last();
        vertices
                .vertex(pose.pose(), -0.5f, 1f, 0f).color(0xFFFFFFFF).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, 1)
                .endVertex();
        vertices
                .vertex(pose.pose(), -0.5f, 0f, 0f).color(0xFFFFFFFF).uv(u1, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, 1)
                .endVertex();
        vertices
                .vertex(pose.pose(), 0.5f, 0f, 0f).color(0xFFFFFFFF).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, 1)
                .endVertex();
        vertices
                .vertex(pose.pose(), 0.5f, 1f, 0f).color(0xFFFFFFFF).uv(u2, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, 1)
                .endVertex();
        vertices
                .vertex(pose.pose(), 0.5f, 1f, 0f).color(0xFFFFFFFF).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, -1)
                .endVertex();
        vertices
                .vertex(pose.pose(), 0.5f, 0f, 0f).color(0xFFFFFFFF).uv(u1, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, -1)
                .endVertex();
        vertices
                .vertex(pose.pose(), -0.5f, 0f, 0f).color(0xFFFFFFFF).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, -1)
                .endVertex();
        vertices
                .vertex(pose.pose(), -0.5f, 1f, 0f).color(0xFFFFFFFF).uv(u2, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 0, -1)
                .endVertex();
        matrices.popPose();
    }

    public static void renderComments(MultiBufferSource buffers, PoseStack matrices) {
        Minecraft minecraft = Minecraft.getInstance();
        for (CommentEntry comment : ClientRayPicking.visibleComments) {
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(ATLAS_LOCATION));
            int light = LightTexture.pack(
                    minecraft.level.getBrightness(LightLayer.BLOCK, comment.location),
                    minecraft.level.getBrightness(LightLayer.SKY, comment.location)
            );
            renderComment(vertices, matrices, comment, light, ClientRayPicking.pickedComments.contains(comment));
        }
    }
}
