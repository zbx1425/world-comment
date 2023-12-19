package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.IGuiCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
#if MC_VERSION >= "11903" import com.mojang.math.Axis; #else import com.mojang.math.Vector3f; #endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CommentWorldRenderer implements IGuiCommon {

    private static final Random RANDOM = new Random();

    public static void renderComment(VertexConsumer vertices, PoseStack matrices, CommentEntry comment,
                                     boolean focused, boolean showIcon) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPos = minecraft.cameraEntity.position();

        RANDOM.setSeed(comment.id);
        matrices.pushPose();
        matrices.translate(comment.location.getX() + 0.5f, comment.location.getY() + 1.8f,
                comment.location.getZ() + 0.5f);
        float cycleRotateLength = 8000;
        float cycleRotateX = ((System.currentTimeMillis() + RANDOM.nextLong(0, (long)cycleRotateLength)) % (long)cycleRotateLength) / cycleRotateLength;
        float cycleRotateY = (float)Math.sin(cycleRotateX * Math.PI * 2) / 2 + 0.5f;
        float cycleHoverLength = focused ? 1000 : 8000;
        float cycleHoverX = ((System.currentTimeMillis() + RANDOM.nextLong(0, (long)cycleHoverLength)) % (long)cycleHoverLength) / cycleHoverLength;
        float cycleHoverY = (float)Math.sin(cycleHoverX * Math.PI * 2) / 2 + 0.5f;
        matrices.translate(
                RANDOM.nextFloat(-0.3f, 0.3f),
                cycleHoverY * 0.1,
                RANDOM.nextFloat(-0.3f, 0.3f)
        );
        float yaw = (float)Mth.atan2(comment.location.getX() + 0.5 - cameraPos.x(), comment.location.getZ() + 0.5 - cameraPos.z());
#if MC_VERSION >= "12000"
        matrices.mulPose(Axis.YP.rotation(yaw + cycleRotateY * (Mth.PI / 24)));
#else
        matrices.mulPose(Vector3f.YP.rotation(yaw + cycleRotateY * (Mth.PI / 24)));
#endif

        int light = LightTexture.FULL_BRIGHT;
        {
            int bgColor = 0xFFFFFFFF;
            matrices.scale(0.9f, 0.9f, 0.9f);
            float u1 = 0.5f, v1 = 0f, u2 = u1 + 0.125f, v2 = v1 + 0.375f;
            PoseStack.Pose pose = matrices.last();
            vertices
                    .vertex(pose.pose(), -0.5f, 1f, 0f).color(bgColor).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, -2f, 0f).color(bgColor).uv(u1, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, -2f, 0f).color(bgColor).uv(u2, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 1f, 0f).color(bgColor).uv(u2, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 1f, 0f).color(bgColor).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, -2f, 0f).color(bgColor).uv(u1, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, -2f, 0f).color(bgColor).uv(u2, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, 1f, 0f).color(bgColor).uv(u2, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
        }

        if (showIcon) {
            matrices.translate(0, 0.25f, 0);
            matrices.scale(0.5f, 0.5f, 0.5f);
            float u1 = ((comment.messageType - 1) % 4) * 0.25f;
            float v1 = (int)((comment.messageType - 1) / 4) * 0.25f + 0.5f;
            float u2 = u1 + 0.25f, v2 = v1 + 0.25f;
            PoseStack.Pose pose = matrices.last();
            vertices
                    .vertex(pose.pose(), -0.5f, 1f, 0.05f).color(0xFFFFFFFF).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, 0f, 0.05f).color(0xFFFFFFFF).uv(u1, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 0f, 0.05f).color(0xFFFFFFFF).uv(u2, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 1f, 0.05f).color(0xFFFFFFFF).uv(u2, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 1f, -0.05f).color(0xFFFFFFFF).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), 0.5f, 0f, -0.05f).color(0xFFFFFFFF).uv(u1, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, 0f, -0.05f).color(0xFFFFFFFF).uv(u2, v2)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
            vertices
                    .vertex(pose.pose(), -0.5f, 1f, -0.05f).color(0xFFFFFFFF).uv(u2, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0)
                    .endVertex();
        }
        matrices.popPose();
    }

    public static void renderComments(MultiBufferSource buffers, PoseStack matrices) {
        long currentTime = System.currentTimeMillis();
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentCull(ATLAS_LOCATION));
        for (Map.Entry<BlockPos, List<CommentEntry>> blockData : ClientRayPicking.visibleComments.entrySet()) {
            for (int i = 0; i < blockData.getValue().size(); i++) {
                CommentEntry comment = blockData.getValue().get(i);
                boolean showIcon = blockData.getValue().size() < 2 ||
                        ((currentTime / 1000) % blockData.getValue().size() == i);
                renderComment(vertices, matrices, comment, ClientRayPicking.pickedComments.contains(comment), showIcon);
            }
        }
    }
}
