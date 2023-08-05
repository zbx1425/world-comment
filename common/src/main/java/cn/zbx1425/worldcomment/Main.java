package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.network.PacketCollectionRequestC2S;
import cn.zbx1425.worldcomment.network.PacketEntryActionC2S;
import cn.zbx1425.worldcomment.network.PacketRegionRequestC2S;
import cn.zbx1425.worldcomment.network.PacketEntryCreateC2S;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {

	public static final String MOD_ID = "worldcomment";
	public static final Logger LOGGER = LoggerFactory.getLogger("Subnoteica");

	public static ServerWorldData DATABASE;

	public static final RegistryObject<Item> ITEM_COMMENT_TOOL = new RegistryObject<>(CommentToolItem::new);

	public static void init(RegistriesWrapper registries) {
		registries.registerItem("comment_tool", ITEM_COMMENT_TOOL, CreativeModeTabs.TOOLS_AND_UTILITIES);

		ServerPlatform.registerNetworkReceiver(
				PacketRegionRequestC2S.IDENTIFIER, PacketRegionRequestC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketCollectionRequestC2S.IDENTIFIER, PacketCollectionRequestC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketEntryCreateC2S.IDENTIFIER, PacketEntryCreateC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketEntryActionC2S.IDENTIFIER, PacketEntryActionC2S::handle);

		ServerPlatform.registerServerStartingEvent(server -> {
			try {
				DATABASE = new ServerWorldData(server);
				DATABASE.load();
			} catch (IOException e) {
				LOGGER.error("Failed to open data storage", e);
				throw new RuntimeException(e);
			}
		});
	}

}
