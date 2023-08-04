package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.render.OverlayLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientProxy {

    public static class ModEventBusListener {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MainClient.init();
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "picked_comments", PICKED_COMMENTS_OVERLAY);
        }

        private static final IGuiOverlay PICKED_COMMENTS_OVERLAY = new PickedCommentsOverlay();

        private static class PickedCommentsOverlay implements IGuiOverlay {

            @Override
            public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float f, int i, int j) {
                OverlayLayer.render(guiGraphics);
            }
        }
    }

    public static class ForgeEventBusListener {

    }
}