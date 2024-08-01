package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.Main;
import net.fabricmc.api.ModInitializer;

public class MainFabric implements ModInitializer {

#if MC_VERSION >= "12100"
	public static final CompatPacketRegistry PACKET_REGISTRY = new CompatPacketRegistry();
#endif

	@Override
	public void onInitialize() {
		Main.init(new RegistriesWrapperImpl());
	}

}
