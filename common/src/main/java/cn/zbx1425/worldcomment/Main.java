package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.Database;
import cn.zbx1425.worldcomment.item.ItemCommentTool;
import cn.zbx1425.worldcomment.util.RegistriesWrapper;
import cn.zbx1425.worldcomment.util.RegistryObject;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class Main {

	public static final String MOD_ID = "worldcomment";

	public static final Logger LOGGER = LoggerFactory.getLogger("Subnoteica");

	public static final RegistryObject<Item> ITEM_COMMENT_TOOL = new RegistryObject<>(ItemCommentTool::new);

	public static void init(RegistriesWrapper registries) {
		registries.registerItem("comment_tool", ITEM_COMMENT_TOOL, CreativeModeTabs.TOOLS_AND_UTILITIES);

		ServerPlatform.registerServerStartingEvent(server -> {
			try {
				Database.loadDatabase(server);
			} catch (SQLException e) {
				LOGGER.error("Failed to open database", e);
				throw new RuntimeException(e);
			}
		});
		ServerPlatform.registerServerStoppingEvent(server -> {
			try {
				Database.INSTANCE.close();
			} catch (SQLException e) {
				LOGGER.error("Failed to close database", e);
				throw new RuntimeException(e);
			}
		});
	}

}
