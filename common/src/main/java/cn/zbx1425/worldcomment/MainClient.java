package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.network.PacketCommentDataUIS2C;
import cn.zbx1425.worldcomment.network.PacketRegionDataS2C;
import cn.zbx1425.worldcomment.network.PacketCommentUpdateS2C;

public class MainClient {

	public static void init() {
		ClientPlatform.registerNetworkReceiver(
				PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketCommentDataUIS2C.IDENTIFIER, PacketCommentDataUIS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketCommentUpdateS2C.IDENTIFIER, PacketCommentUpdateS2C.ClientLogics::handle);
	}

}
