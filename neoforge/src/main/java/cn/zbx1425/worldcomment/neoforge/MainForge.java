package cn.zbx1425.worldcomment.neoforge;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.ServerCommand;
import net.minecraft.commands.Commands;
#if MC_VERSION >= "12100"
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
#else
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
#endif

@Mod(Main.MOD_ID)
public class MainForge {

	private static final RegistriesWrapperImpl registries = new RegistriesWrapperImpl();
#if MC_VERSION >= "12100"
	public static final CompatPacketRegistry PACKET_REGISTRY = new CompatPacketRegistry();
#endif

	static {
		Main.init(registries);
	}

#if MC_VERSION >= "12100"
	public MainForge(IEventBus eventBus) {
#else
	public MainForge() {
		final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
		EventBuses.registerModEventBus(Main.MOD_ID, eventBus);
#endif
		registries.registerAllDeferred(eventBus);
		eventBus.register(RegistriesWrapperImpl.RegisterCreativeTabs.class);
		NeoForge.EVENT_BUS.register(ForgeEventBusListener.class);

#if MC_VERSION >= "12100"
		eventBus.register(ModEventBusListener.class);
		if (FMLEnvironment.dist.isClient()) {
#else
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
#endif
			eventBus.register(ClientProxy.ModEventBusListener.class);
#if MC_VERSION >= "12100"
			NeoForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
		}
#else
			MinecraftForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
		});
#endif
	}

	public static class ModEventBusListener {

#if MC_VERSION >= "12100"
		@SubscribeEvent
		public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			MainForge.PACKET_REGISTRY.commit(registrar);
		}
#endif
	}

	public static class ForgeEventBusListener {

		@SubscribeEvent
		public static void onRegisterCommand(RegisterCommandsEvent event) {
			ServerCommand.register(event.getDispatcher(), Commands::literal, Commands::argument);
		}
	}
}
