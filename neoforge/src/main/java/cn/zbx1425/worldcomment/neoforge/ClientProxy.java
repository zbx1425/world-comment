package cn.zbx1425.worldcomment.neoforge;

import cn.zbx1425.worldcomment.ClientCommand;
import cn.zbx1425.worldcomment.ClientConfig;
import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.client.ClientRayPicking;
import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import cn.zbx1425.worldcomment.render.OverlayLayer;
#if MC_VERSION >= "12000"
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphicsExtractor; #endif
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
#if MC_VERSION < "12002" import net.minecraft.client.gui.LayeredDraw; #endif
#if MC_VERSION >= "12002" import net.neoforged.neoforge.client.gui.GuiLayer; #endif
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
#if MC_VERSION >= "12102" import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent; #endif
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class ClientProxy {

    public static class ModEventBusListener {
#if MC_VERSION >= "12100"
        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.SCOREBOARD_SIDEBAR, Main.id("picked_comments"), PICKED_COMMENTS_OVERLAY);
        }

        private static final #if MC_VERSION >= "12002" GuiLayer #else LayeredDraw.Layer #endif PICKED_COMMENTS_OVERLAY = new PickedCommentsOverlay();

        private static class PickedCommentsOverlay implements #if MC_VERSION >= "12002" GuiLayer #else LayeredDraw.Layer #endif {

            @Override
            public void render(GuiGraphicsExtractor guiParam, DeltaTracker deltaTracker) {
                OverlayLayer.render(ISnGuiGraphics.fromGuiParam(guiParam));
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
            public void render(ForgeGui forgeGui, #if MC_VERSION >= "12000" GuiGraphicsExtractor #else PoseStack #endif guiGraphics, float f, int i, int j) {
                OverlayLayer.render(#if MC_VERSION >= "12000" guiGraphics #else GuiGraphicsExtractor.withPose(guiGraphics) #endif);
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
        public static void onClientSetupEvent(FMLClientSetupEvent event) {
            MainClient.init();
        }

#if MC_VERSION >= "12102"
        @SubscribeEvent
        private static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
            MainForge.PACKET_REGISTRY.commitClient(event);
        }
#endif
    }

    public static class ForgeEventBusListener {

        private static boolean world_comment$lastFrameKeyPlayerListDown = false;

#if MC_VERSION >= "12102"
        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentFeatures event) { {
#else
        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
#endif
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
                Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
                matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                CommentWorldRenderer.renderComments(Minecraft.getInstance().renderBuffers().bufferSource(), matrices);
                matrices.popPose();
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            MainClient.CLIENT_CONFIG.tick(1, 0);
        }

        @SubscribeEvent
        public static void onRegisterClientCommand(RegisterCommandsEvent event) {
            ClientCommand.register(event.getDispatcher(), Commands::literal, Commands::argument);
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