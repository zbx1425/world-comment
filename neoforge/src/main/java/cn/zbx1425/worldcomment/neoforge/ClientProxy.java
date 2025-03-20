package cn.zbx1425.worldcomment.neoforge;

import cn.zbx1425.worldcomment.ClientCommand;
import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import cn.zbx1425.worldcomment.render.OverlayLayer;
#if MC_VERSION >= "12000"
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; #endif
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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

        @SubscribeEvent
        public static void onRegisterClientCommand(RegisterCommandsEvent event) {
            ClientCommand.register(event.getDispatcher(), Commands::literal, Commands::argument);
        }

        @SubscribeEvent
        public static void onClientSetupEvent(FMLClientSetupEvent event) {
            MainClient.init();
        }
    }

    public static class ForgeEventBusListener {

        private static boolean world_comment$lastFrameKeyPlayerListDown = false;

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                if (Minecraft.getInstance().options.keyPlayerList.isDown()) {
                    if (!world_comment$lastFrameKeyPlayerListDown) {
                        CommentListScreen.handleKeyTab();
                    }
                    world_comment$lastFrameKeyPlayerListDown = true;
                } else {
                    world_comment$lastFrameKeyPlayerListDown = false;
                }

                PoseStack matrices = event.getPoseStack();
                matrices.pushPose();
                Vec3 cameraPos = event.getCamera().getPosition();
                matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                CommentWorldRenderer.renderComments(Minecraft.getInstance().renderBuffers().bufferSource(), matrices);
                matrices.popPose();
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            MainClient.CLIENT_CONFIG.tick(1, 0);
        }
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