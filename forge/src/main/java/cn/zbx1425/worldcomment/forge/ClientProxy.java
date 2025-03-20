package cn.zbx1425.worldcomment.forge;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.ClientCommand;
import cn.zbx1425.worldcomment.render.OverlayLayer;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;

public class ClientProxy {

    public static class ModEventBusListener {

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

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (KeyMapping keyMapping : ClientPlatformImpl.KEY_MAPPINGS) {
                keyMapping.setKeyConflictContext(NoConflictKeyConflictContext.INSTANCE);
                event.register(keyMapping);
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommand(RegisterCommandsEvent event) {
            ClientCommand.register(event.getDispatcher(), Commands::literal, Commands::argument);
        }
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