package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.MainClient;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class MainFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MainClient.init();

		HudRenderCallback.EVENT.register((guiGraphics, delta) -> {
			OverlayLayer.render(#if MC_VERSION >= "12000" guiGraphics #else GuiGraphics.withPose(guiGraphics) #endif);
		});
	}

}