package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.network.PacketRegionDataS2C;

public class MainClient {

	public static void init() {
		ClientPlatform.registerNetworkReceiver(PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);
	}

}
