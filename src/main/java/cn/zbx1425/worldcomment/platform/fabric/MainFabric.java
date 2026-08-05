package cn.zbx1425.worldcomment.platform.fabric;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class MainFabric implements ModInitializer {

//? if >=1.21 {
	public static final CompatPacketRegistry PACKET_REGISTRY = new CompatPacketRegistry();
//? }

	@Override
	public void onInitialize() {
		Main.init(new RegistriesWrapperImpl());
//? if >=1.21 {
		PACKET_REGISTRY.commitCommon();
//? }
		CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) ->
				ServerCommand.register(commandDispatcher, Commands::literal, Commands::argument));
	}

}
