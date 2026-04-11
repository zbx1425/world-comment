package cn.zbx1425.worldcomment.render;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.gui.IGuiCommon;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
#if MC_VERSION >= "11903" import com.mojang.math.Axis; #else import com.mojang.math.Vector3f; #endif
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class CommentWorldRenderer implements IGuiCommon {

    private static final Random RANDOM = new Random();

    public static void renderComment(VertexConsumer vertices, PoseStack matrices, CommentEntry comment,
                                     boolean focused, boolean showIcon) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPos = minecraft.getCameraEntity().position();

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
        float randomXOff = RANDOM.nextFloat(-0.3f, 0.3f), randomZOff = RANDOM.nextFloat(-0.3f, 0.3f);
        matrices.translate(randomXOff, cycleHoverY * 0.1, randomZOff);
        float yaw = (float)Mth.atan2(comment.location.getX() + 0.5 + randomXOff - cameraPos.x(), comment.location.getZ() + 0.5 + randomZOff - cameraPos.z());
#if MC_VERSION >= "12000"
        matrices.mulPose(Axis.YP.rotation(yaw + cycleRotateY * (Mth.PI / 24)));
#else
        matrices.mulPose(Vector3f.YP.rotation(yaw + cycleRotateY * (Mth.PI / 24)));
#endif

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        {
            TextureAtlasSprite poleSprite = EmojiRegistry.INSTANCE.getPoleSprite();
            matrices.scale(0.9f, 0.9f, 0.9f);
            PoseStack.Pose pose = matrices.last().copy();

            vertex(vertices, pose, 0.5f, 0.1875f, 0f, poleSprite.getU0(), poleSprite.getV0());
            vertex(vertices, pose, 0.5f, -1.8125f, 0f, poleSprite.getU0(), poleSprite.getV1());
            vertex(vertices, pose, -0.5f, -1.8125f, 0f, poleSprite.getU(31 / 32f), poleSprite.getV1());
            vertex(vertices, pose, -0.5f, 0.1875f, 0f, poleSprite.getU(31 / 32f), poleSprite.getV0());
        }

        if (showIcon) {
            TextureAtlasSprite iconSprite = EmojiRegistry.INSTANCE.getSprite(comment.messageType);
            matrices.translate(0, 0.75f, 0);

            Matrix3f cameraRotMat = new Matrix3f();
            camera.rotation().get(cameraRotMat);
            PoseStack.Pose pose = matrices.last();
            pose.pose().set3x3(cameraRotMat).scale(0.8f);
            pose.normal().identity().scale(1, -1, 1);

            vertex(vertices, pose, 0.5f, 0.5f, -0.05f, iconSprite.getU0(), iconSprite.getV0());
            vertex(vertices, pose, 0.5f, -0.5f, -0.05f, iconSprite.getU0(), iconSprite.getV1());
            vertex(vertices, pose, -0.5f, -0.5f, -0.05f, iconSprite.getU1(), iconSprite.getV1());
            vertex(vertices, pose, -0.5f, 0.5f, -0.05f, iconSprite.getU1(), iconSprite.getV0());
        }
        matrices.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
#if MC_VERSION >= "12100"
        vertices.addVertex(pose.pose(), x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
#else
        vertices.vertex(pose.pose(), x, y, z).color(0xFFFFFFFF).uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
#endif
    }

    public static void renderComments(MultiBufferSource buffers, PoseStack matrices) {
        long currentTime = System.currentTimeMillis();
        VertexConsumer vertices = buffers.getBuffer(RenderTypes.entityTranslucent(EmojiRegistry.ATLAS_TEXTURE_ID));
        for (Map.Entry<BlockPos, List<CommentEntry>> blockData : ClientRayPicking.visibleComments.entrySet()) {
            for (int i = 0; i < blockData.getValue().size(); i++) {
                CommentEntry comment = blockData.getValue().get(i);
                boolean isVisible = MainClient.CLIENT_CONFIG.isCommentVisible(Minecraft.getInstance(), comment);
                if (!isVisible) continue;
                boolean showIcon = blockData.getValue().size() < 2 ||
                        ((currentTime / 1000) % blockData.getValue().size() == i);
                renderComment(vertices, matrices, comment, ClientRayPicking.pickedComments.contains(comment), showIcon);
            }
        }
    }
}
