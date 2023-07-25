package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.network.PacketRegionDataS2C;
import cn.zbx1425.worldcomment.network.PacketRegionUpdateS2C;

public class MainClient {

	public static void init() {
		ClientPlatform.registerNetworkReceiver(PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(PacketRegionUpdateS2C.IDENTIFIER, PacketRegionUpdateS2C.ClientLogics::handle);
	}

}
