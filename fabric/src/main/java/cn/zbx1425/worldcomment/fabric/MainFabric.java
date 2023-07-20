package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.Main;
import net.fabricmc.api.ModInitializer;

public class MainFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		Main.init();
	}

}
