package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.MainClient;
#if MC_VERSION >= "12000" import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.world.phys.Vec3;

public class MainFabricClient implements ClientModInitializer {

	private static boolean world_comment$lastFrameKeyPlayerListDown = false;

	@Override
	public void onInitializeClient() {
		MainClient.init();

		HudRenderCallback.EVENT.register((guiGraphics, delta) -> {
			OverlayLayer.render(#if MC_VERSION >= "12000" guiGraphics #else GuiGraphics.withPose(guiGraphics) #endif);
		});

		WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
			ClientWorldData.INSTANCE.tick();
			ClientRayPicking.tick(context.tickCounter().getGameTimeDeltaTicks(), 20);

			if (Minecraft.getInstance().options.keyPlayerList.isDown()) {
				if (!world_comment$lastFrameKeyPlayerListDown) {
					CommentListScreen.handleKeyTab();
				}
				world_comment$lastFrameKeyPlayerListDown = true;
			} else {
				world_comment$lastFrameKeyPlayerListDown = false;
			}

			PoseStack matrices = context.matrixStack();
			matrices.pushPose();
			Vec3 cameraPos = context.camera().getPosition();
			matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			CommentWorldRenderer.renderComments(Minecraft.getInstance().renderBuffers().bufferSource(), matrices);
			matrices.popPose();
		});
	}

}