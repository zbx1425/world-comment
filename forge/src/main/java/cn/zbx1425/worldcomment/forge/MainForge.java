package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Main.MOD_ID)
public class MainForge {

	private static final RegistriesWrapperImpl registries = new RegistriesWrapperImpl();

	static {
		Main.init(registries);
	}

	public MainForge() {
		final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
		EventBuses.registerModEventBus(Main.MOD_ID, eventBus);

		registries.registerAllDeferred();
		eventBus.register(RegistriesWrapperImpl.RegisterCreativeTabs.class);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			MainClient.init();
			eventBus.register(ClientProxy.ModEventBusListener.class);
			MinecraftForge.EVENT_BUS.register(ClientProxy.ForgeEventBusListener.class);
		});
	}

}
