package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.client.ClientDatabase;
import cn.zbx1425.worldcomment.network.PacketCollectionDataS2C;
import cn.zbx1425.worldcomment.network.PacketRegionDataS2C;
import cn.zbx1425.worldcomment.network.PacketEntryUpdateS2C;

public class MainClient {

	public static void init() {
		ClientPlatform.registerNetworkReceiver(
				PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketCollectionDataS2C.IDENTIFIER, PacketCollectionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketEntryUpdateS2C.IDENTIFIER, PacketEntryUpdateS2C.ClientLogics::handle);

		ClientPlatform.registerPlayerJoinEvent(ignored -> {
			ClientDatabase.INSTANCE.clear();
		});
	}

}
