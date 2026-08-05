package cn.zbx1425.worldcomment.platform.fabric;

import cn.zbx1425.worldcomment.ClientCommand;
import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.util.KeyMappingUtil;
import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
//? if >=1.20
import cn.zbx1425.worldcomment.gui.compat.ISnGuiCanvas;
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

//? if >=1.21 {
		MainFabric.PACKET_REGISTRY.commitClient();
//? }

		HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD, Main.id("picked_comments"),
			(guiParam, deltaTracker) -> OverlayLayer.render(ISnGuiCanvas.fromGuiParam(guiParam)));

//? if >=1.21 {
		// TODO: Correct timing?
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((context) -> {
			if (KeyMappingUtil.isKeyDown(Minecraft.getInstance().options.keyPlayerList)) {
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
//? }

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