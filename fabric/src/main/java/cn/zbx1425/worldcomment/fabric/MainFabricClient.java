package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.MainClient;
import net.fabricmc.api.ClientModInitializer;

public class MainFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MainClient.init();
	}

}