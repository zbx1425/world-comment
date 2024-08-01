package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;
#if MC_VERSION >= "11903" import org.joml.Matrix4f; #else import com.mojang.math.Matrix4f; #endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final
    private RenderBuffers renderBuffers;

#if MC_VERSION < "12100"
    @Unique
    private boolean world_comment$lastFrameKeyPlayerListDown = false;

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0))
    private void afterEntities(PoseStack matrices, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        ClientWorldData.INSTANCE.tick();
        ClientRayPicking.tick(partialTick, 20);

        if (Minecraft.getInstance().options.keyPlayerList.isDown()) {
            if (!world_comment$lastFrameKeyPlayerListDown) {
                CommentListScreen.handleKeyTab();
            }
            world_comment$lastFrameKeyPlayerListDown = true;
        } else {
            world_comment$lastFrameKeyPlayerListDown = false;
        }

        matrices.pushPose();
        Vec3 cameraPos = camera.getPosition();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        CommentWorldRenderer.renderComments(renderBuffers.bufferSource(), matrices);
        matrices.popPose();
    }
#endif
}
