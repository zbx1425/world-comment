package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
#if MC_VERSION >= "12100"
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
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

#if MC_VERSION >= "12100"
		if (FMLEnvironment.dist.isClient()) {
#else
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
#endif
			MainClient.init();
			eventBus.register(ClientProxy.ModEventBusListener.class);
#if MC_VERSION >= "12100"
			NeoForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
		}
#else
			MinecraftForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
		});
#endif
	}

}
