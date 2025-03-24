package cn.zbx1425.worldcomment.fabric;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class MainFabric implements ModInitializer {

#if MC_VERSION >= "12100"
	public static final CompatPacketRegistry PACKET_REGISTRY = new CompatPacketRegistry();
#endif

	@Override
	public void onInitialize() {
		Main.init(new RegistriesWrapperImpl());
#if MC_VERSION >= "12100"
		PACKET_REGISTRY.commitCommon();
#endif
		CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) ->
				ServerCommand.register(commandDispatcher, Commands::literal, Commands::argument));
	}

}
