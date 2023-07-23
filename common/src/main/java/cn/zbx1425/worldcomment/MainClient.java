package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.client.ClientDatabase;
import cn.zbx1425.worldcomment.network.PacketRegionDataS2C;

public class MainClient {

	public static void init() {
		ClientPlatform.registerNetworkReceiver(PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);

		ClientPlatform.registerTickEvent(ignored -> {
			ClientDatabase.INSTANCE.tick();
		});
	}

}
