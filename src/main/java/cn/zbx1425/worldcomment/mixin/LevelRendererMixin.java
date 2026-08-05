package cn.zbx1425.worldcomment.mixin;

import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import cn.zbx1425.worldcomment.util.KeyMappingUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;
//? if >=1.19.3
import org.joml.Matrix4f;
//? if <1.19.3
//import com.mojang.math.Matrix4f;
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

//? if <1.21 {
    /*@Unique
    private boolean world_comment$lastFrameKeyPlayerListDown = false;

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0))
    private void afterEntities(PoseStack matrices, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (KeyMappingUtil.isKeyDown(Minecraft.getInstance().options.keyPlayerList)) {
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
*///? }
}
