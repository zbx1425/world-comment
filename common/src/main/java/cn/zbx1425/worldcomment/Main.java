package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.sync.RedisSynchronizer;
import cn.zbx1425.worldcomment.item.CommentEyeglassItem;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import cn.zbx1425.worldcomment.item.GroupedItem;
import cn.zbx1425.worldcomment.network.*;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {

	public static final String MOD_ID = "worldcomment";
	public static final Logger LOGGER = LoggerFactory.getLogger("Subnoteica");

	public static ServerWorldData DATABASE;

	public static ServerConfig SERVER_CONFIG = new ServerConfig();

	public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	public static final Executor IO_EXECUTOR = Executors.newCachedThreadPool();

	public static final RegistryObject<CommentToolItem> ITEM_COMMENT_TOOL = new RegistryObject<>(CommentToolItem::new);
	public static final RegistryObject<CommentEyeglassItem> ITEM_COMMENT_EYEGLASS = new RegistryObject<>(CommentEyeglassItem::new);

	public static void init(RegistriesWrapper registries) {
		registries.registerItem("comment_tool", ITEM_COMMENT_TOOL);
		registries.registerItem("comment_eyeglass", ITEM_COMMENT_EYEGLASS);

		ServerPlatform.registerPacket(PacketClientConfigS2C.IDENTIFIER);
		ServerPlatform.registerPacket(PacketCollectionDataS2C.IDENTIFIER);
		ServerPlatform.registerPacket(PacketCollectionRequestC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketEntryActionC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketEntryCreateC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketEntryUpdateS2C.IDENTIFIER);
		ServerPlatform.registerPacket(PacketRegionDataS2C.IDENTIFIER);
		ServerPlatform.registerPacket(PacketRegionRequestC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketImageUploadC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketImageUploadS2C.IDENTIFIER);
		ServerPlatform.registerPacket(PacketImageDownloadC2S.IDENTIFIER);
		ServerPlatform.registerPacket(PacketImageDownloadS2C.IDENTIFIER);

		ServerPlatform.registerNetworkReceiver(
				PacketRegionRequestC2S.IDENTIFIER, PacketRegionRequestC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketCollectionRequestC2S.IDENTIFIER, PacketCollectionRequestC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketEntryCreateC2S.IDENTIFIER, PacketEntryCreateC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketEntryActionC2S.IDENTIFIER, PacketEntryActionC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketImageUploadC2S.IDENTIFIER, PacketImageUploadC2S::handle);
		ServerPlatform.registerNetworkReceiver(
				PacketImageDownloadC2S.IDENTIFIER, PacketImageDownloadC2S::handle);

		ServerPlatform.registerServerStartingEvent(server -> {
			try {
#if MC_VERSION >= "12100"
				SERVER_CONFIG.load(server.getServerDirectory()
#else
				SERVER_CONFIG.load(server.getServerDirectory().toPath()
#endif
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

	public static ResourceLocation id(String path) {
#if MC_VERSION >= "12100"
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
#else
		return new ResourceLocation(MOD_ID, path);
#endif
	}

	public static ResourceLocation vanillaId(String path) {
#if MC_VERSION >= "12100"
		return ResourceLocation.withDefaultNamespace(path);
#else
		return new ResourceLocation(path);
#endif
	}

}
