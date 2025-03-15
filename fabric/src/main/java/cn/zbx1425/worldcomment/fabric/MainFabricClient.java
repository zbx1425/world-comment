package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.MainClient;
#if MC_VERSION >= "12000"import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.world.phys.Vec3;

public class MainFabricClient implements ClientModInitializer {

	private static boolean world_comment$lastFrameKeyPlayerListDown = false;

	@Override
	public void onInitializeClient() {
		MainClient.init();

#if MC_VERSION >= "12100"
		MainFabric.PACKET_REGISTRY.commitClient();
#endif

		HudRenderCallback.EVENT.register((guiGraphics, delta) -> {
			OverlayLayer.render(#if MC_VERSION >= "12000" guiGraphics #else GuiGraphics.withPose(guiGraphics) #endif);
		});

#if MC_VERSION >= "12100"
		WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
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
#endif

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			MainClient.CLIENT_CONFIG.tick(1, 0);
		});
	}
}