package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.sync.RedisSynchronizer;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.mixin.CreativeModeTabsAccessor;
import cn.zbx1425.worldcomment.network.*;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {

	public static final String MOD_ID = "worldcomment";
	public static final Logger LOGGER = LoggerFactory.getLogger("Subnoteica");

	public static ServerWorldData DATABASE;

	public static ServerConfig SERVER_CONFIG = new ServerConfig();

	public static final RegistryObject<Item> ITEM_COMMENT_TOOL = new RegistryObject<>(CommentToolItem::new);

	public static void init(RegistriesWrapper registries) {
		registries.registerItem("comment_tool", ITEM_COMMENT_TOOL, CreativeModeTabsAccessor.getTOOLS_AND_UTILITIES());

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
				SERVER_CONFIG.load(server.getServerDirectory().toPath()
						.resolve("config").resolve("world-comment.json"));

				DATABASE = new ServerWorldData(server, SERVER_CONFIG.syncRole.value.equalsIgnoreCase("host"));
				if (!SERVER_CONFIG.redisUrl.value.isEmpty()) {
					DATABASE.peerChannel = new RedisSynchronizer(SERVER_CONFIG.redisUrl.value, DATABASE);
				}
				DATABASE.load();
			} catch (IOException e) {
				LOGGER.error("Failed to open data storage", e);
				throw new RuntimeException(e);
			}
		});
		ServerPlatform.registerServerStoppingEvent(server -> {
			try {
				DATABASE.peerChannel.close();
			} catch (Exception ex) {
				LOGGER.error("Failed to close database peerChannel", ex);
			}
		});

		ServerPlatform.registerPlayerJoinEvent(player -> {
			PacketClientConfigS2C.send(player, SERVER_CONFIG);
		});
	}

}
