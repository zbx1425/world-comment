package cn.zbx1425.worldcomment.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
#if MC_VERSION >= "12000" import net.minecraft.client.gui.GuiGraphics; #else import cn.zbx1425.worldcomment.util.compat.GuiGraphics; import com.mojang.blaze3d.vertex.PoseStack; #endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

#if MC_VERSION >= "12000" import java.util.function.Supplier; #endif

public class CommentTypeButton extends Button implements IGuiCommon {

    public static final int[] COMMENT_TYPE_COLOR = {
        0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFF9800,
        0xFF607D8B, 0xFFFFC107, 0xFF03A9F4, 0xFF009888
    };

    public int commentType;
    public int topColor;

    public static int BTN_WIDTH = 40;
    public static int BTN_HEIGHT = 40;

    public CommentTypeButton(int x, int y, int type, OnPress onPress) {
        super(x, y, BTN_WIDTH, BTN_HEIGHT,
                Component.translatable("gui.worldcomment.comment_type." + type),
                onPress #if MC_VERSION >= "12000" , Supplier::get #endif);
        this.commentType = type;
        this.topColor = COMMENT_TYPE_COLOR[type - 1];
    }

    @Override
#if MC_VERSION >= "12000"
    protected void renderWidget(GuiGraphics guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = guiParam;
#else
    public void render(PoseStack guiParam, int mouseX, int mouseY, float partialTick) {
        final GuiGraphics guiGraphics = GuiGraphics.withPose(guiParam);
        super.render(guiParam, mouseX, mouseY, partialTick);
#endif
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableBlend();
        guiGraphics.setColor(((topColor >> 16) & 0xFF) / 255f, ((topColor >> 8) & 0xFF) / 255f,
                (topColor & 0xFF) / 255f, 1);
        guiGraphics.blit(ATLAS_LOCATION, getX(), getY(), getWidth(), 12,
                active ? 0 : 40, 58, 40, 12, 256, 256);
        guiGraphics.setColor(1, 1, 1, 1);
        guiGraphics.blit(ATLAS_LOCATION, getX(), getY() + 12, getWidth(), 28,
                active ? 0 : 40, 70, 40, 28, 256, 256);
        if (isHovered && active) {
            guiGraphics.blit(ATLAS_LOCATION, getX(), getY(), getWidth(), 40,
                    80, 58, 40, 40, 256, 256);
        }
        guiGraphics.blit(ATLAS_LOCATION, getX() + 8 - 1, getY() + 14 - 1, 24, 24,
                ((commentType - 1) % 4) * 64, (int)((commentType - 1) / 4) * 64 + 128, 64, 64, 256, 256);
        renderScrollingString(guiGraphics, minecraft.font, getMessage(),
                getX(), getY(), getX() + getWidth(), getY() + 12, active ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

#if MC_VERSION < "12000"
    private int getX() { return x; }
    private int getY() { return y; }
#endif
}
