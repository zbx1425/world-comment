package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.ClientCommand;
import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
#if MC_VERSION >= "12000" import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; #endif
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.phys.Vec3;

public class MainFabricClient implements ClientModInitializer {

	private static boolean world_comment$lastFrameKeyPlayerListDown = false;

	@Override
	public void onInitializeClient() {
		MainClient.init();

#if MC_VERSION >= "12100"
		MainFabric.PACKET_REGISTRY.commitClient();
#endif

		HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD, Main.id("picked_comments"),
			(guiParam, deltaTracker) -> OverlayLayer.render(ISnGuiGraphics.fromGuiParam(guiParam)));

#if MC_VERSION >= "12100"
		// TODO: Correct timing?
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((context) -> {
			if (Minecraft.getInstance().options.keyPlayerList.isDown()) {
				if (!world_comment$lastFrameKeyPlayerListDown) {
					CommentListScreen.handleKeyTab();
				}
				world_comment$lastFrameKeyPlayerListDown = true;
			} else {
				world_comment$lastFrameKeyPlayerListDown = false;
			}

			PoseStack matrices = context.poseStack();
			matrices.pushPose();
			Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
			matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			CommentWorldRenderer.renderComments(Minecraft.getInstance().renderBuffers().bufferSource(), matrices);
			matrices.popPose();
		});
#endif

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			MainClient.CLIENT_CONFIG.tick(1, 0);
		});

		ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext) -> {
			ClientCommand.register(commandDispatcher, LiteralArgumentBuilder::literal, RequiredArgumentBuilder::argument);
		});

		
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(Main.MOD_ID, "emoji_atlas"),
			EmojiRegistry.INSTANCE
		);
	}
}