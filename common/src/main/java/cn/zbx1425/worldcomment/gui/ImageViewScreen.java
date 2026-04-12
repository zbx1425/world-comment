package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.data.network.ImageDownload;
import cn.zbx1425.worldcomment.data.network.ThumbImage;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ImageViewScreen extends Screen {

    private final Screen parent;
    private final ThumbImage image;

    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;

    private int imgNativeW = 16;
    private int imgNativeH = 9;
    private double fitZoom = 1.0;
    private boolean initialized = false;

    protected ImageViewScreen(Screen parent, ThumbImage image) {
        super(Component.literal(""));
        this.parent = parent;
        this.image = image;
    }

    private void recalculateFit() {
        double scaleX = (double) (width - 40) / imgNativeW;
        double scaleY = (double) (height - 40) / imgNativeH;
        fitZoom = Math.min(scaleX, scaleY);
    }

    @Override
    protected void init() {
        super.init();
        ImageDownload.ImageState state = ImageDownload.getTexture(image, false);
        imgNativeW = state.width;
        imgNativeH = state.height;
        recalculateFit();

        if (!initialized) {
            zoom = fitZoom;
            panX = (width - imgNativeW * zoom) / 2.0;
            panY = (height - imgNativeH * zoom) / 2.0;
            initialized = true;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiGraphics g = ISnGuiGraphics.fromGuiParam(guiParam);
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiParam, int mouseX, int mouseY, float partialTick) {
        ISnGuiGraphics g = ISnGuiGraphics.fromGuiParam(guiParam);
        Minecraft mc = Minecraft.getInstance();

        super.extractRenderState(guiParam, mouseX, mouseY, partialTick);

        ImageDownload.ImageState state = ImageDownload.getTexture(image, false);
        if (state.width != imgNativeW || state.height != imgNativeH) {
            imgNativeW = state.width;
            imgNativeH = state.height;
            recalculateFit();
            zoom = fitZoom;
            panX = (width - imgNativeW * zoom) / 2.0;
            panY = (height - imgNativeH * zoom) / 2.0;
        }

        int drawW = (int) (imgNativeW * zoom);
        int drawH = (int) (imgNativeH * zoom);
        int drawX = (int) panX;
        int drawY = (int) panY;

        g.blit(state.getFriendlyTexture(mc.getTextureManager()),
                drawX, drawY, drawX + drawW, drawY + drawH);

        String zoomStr = String.format("%.0f%%", zoom / fitZoom * 100);
        g.drawString(mc.font, zoomStr, 8, height - 16, 0xAAFFFFFF, true);

        Component hints = Component.translatable("gui.worldcomment.imageview.hints");
        int hintsWidth = mc.font.width(hints);
        g.drawString(mc.font, hints, width - hintsWidth - 8, height - 16, 0xAA888888, true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double oldZoom = zoom;
        double factor = deltaY > 0 ? 1.15 : (1.0 / 1.15);
        double minZoom = fitZoom * 0.5;
        double maxZoom = Math.max(fitZoom * 20, 10.0);
        zoom = Mth.clamp(zoom * factor, minZoom, maxZoom);

        panX = mouseX - (mouseX - panX) * (zoom / oldZoom);
        panY = mouseY - (mouseY - panY) * (zoom / oldZoom);

        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        panX += dx;
        panY += dy;
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return false;
    }
}
