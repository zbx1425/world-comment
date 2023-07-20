package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class Main {

	public static final String MOD_ID = "worldcomment";

	public static final Logger LOGGER = LoggerFactory.getLogger("Subnoteica");

	public static void init() {
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
