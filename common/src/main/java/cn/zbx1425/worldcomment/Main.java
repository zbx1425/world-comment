package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.persist.Database;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.network.PacketRequestCommentUIC2S;
import cn.zbx1425.worldcomment.network.PacketRequestRegionC2S;
import cn.zbx1425.worldcomment.network.PacketSubmitCommentC2S;
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

	public static Database DATABASE;

	public static final RegistryObject<Item> ITEM_COMMENT_TOOL = new RegistryObject<>(CommentToolItem::new);

	public static void init(RegistriesWrapper registries) {
		registries.registerItem("comment_tool", ITEM_COMMENT_TOOL, CreativeModeTabs.TOOLS_AND_UTILITIES);

		ServerPlatform.registerNetworkReceiver(
				PacketRequestRegionC2S.IDENTIFIER, PacketRequestRegionC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketRequestCommentUIC2S.IDENTIFIER, PacketRequestCommentUIC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketSubmitCommentC2S.IDENTIFIER, PacketSubmitCommentC2S::handle);

		ServerPlatform.registerServerStartingEvent(server -> {
			try {
				DATABASE = new Database(server);
				DATABASE.load();
			} catch (IOException e) {
				LOGGER.error("Failed to open data storage", e);
				throw new RuntimeException(e);
			}
		});
	}

}
