package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientProxy {

    public static class ModEventBusListener {

        @SubscribeEvent
        public static void onClientSetupEvent(FMLClientSetupEvent event) {
            MainClient.init();
        }

    }

    public static class ForgeEventBusListener {

        @SubscribeEvent
        public static void onRenderGameOverlay(RenderGuiOverlayEvent.Post event) {
            OverlayLayer.render(event.getGuiGraphics());
        }
    }
}