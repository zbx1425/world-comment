package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.render.OverlayLayer;
#if MC_VERSION >= "12000" import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
#if MC_VERSION >= "12100"
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
#else
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
#endif

public class ClientProxy {

    public static class ModEventBusListener {
#if MC_VERSION >= "12100"
        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.SCOREBOARD_SIDEBAR, Main.id("picked_comments"), PICKED_COMMENTS_OVERLAY);
        }

        private static final LayeredDraw.Layer PICKED_COMMENTS_OVERLAY = new PickedCommentsOverlay();

        private static class PickedCommentsOverlay implements LayeredDraw.Layer {

            @Override
            public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
                OverlayLayer.render(guiGraphics);
            }
        }
#else
        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.SCOREBOARD.id(), "picked_comments", PICKED_COMMENTS_OVERLAY);
        }

        private static final IGuiOverlay PICKED_COMMENTS_OVERLAY = new PickedCommentsOverlay();

        private static class PickedCommentsOverlay implements IGuiOverlay {

            @Override
            public void render(ForgeGui forgeGui, #if MC_VERSION >= "12000" GuiGraphics #else PoseStack #endif guiGraphics, float f, int i, int j) {
                OverlayLayer.render(#if MC_VERSION >= "12000" guiGraphics #else GuiGraphics.withPose(guiGraphics) #endif);
            }
        }
#endif

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (KeyMapping keyMapping : ClientPlatformImpl.KEY_MAPPINGS) {
                keyMapping.setKeyConflictContext(NoConflictKeyConflictContext.INSTANCE);
                event.register(keyMapping);
            }
        }

#if MC_VERSION >= "12100"
        @SubscribeEvent
        public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            MainForge.PACKET_REGISTRY.commit(registrar);
        }
#endif
    }

    public static class ForgeEventBusListener {

    }

    private static class NoConflictKeyConflictContext implements IKeyConflictContext {

        public static NoConflictKeyConflictContext INSTANCE = new NoConflictKeyConflictContext();

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean conflicts(IKeyConflictContext iKeyConflictContext) {
            return false;
        }
    }
}