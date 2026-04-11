package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import cn.zbx1425.worldcomment.gui.compat.ISnGuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.IntConsumer;

public class WidgetEmojiPanel extends AbstractContainerWidget implements IGuiCommon {

    private static final int ITEM_SIZE = 20;
    private static final int ITEM_SPACING_X = 10;
    private static final int ITEM_SPACING_Y = 10;
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 8;

    private static final int ITEM_PADDING = 2;

    private int columns;
    private int realPaddingX;
    private int contentHeight;
    private int selectedId;

    private IntConsumer onSelectionChange;

    public WidgetEmojiPanel(int width, int height, IntConsumer onSelectionChange) {
        super(0, 0, width, height, CommonComponents.EMPTY, defaultSettings(ITEM_SIZE / 2));
        this.onSelectionChange = onSelectionChange;
        this.repositionEntries();
        this.refreshScrollAmount();
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        ISnGuiGraphics guiGraphics = ISnGuiGraphics.fromGuiParam(graphics);
        guiGraphics.blitNineSlicedFast(
            ATLAS_LOCATION,
            getX(), getY(), width - scrollbarWidth(), height,
            40, 58, 40, 40, 256, 256, 8, 8, 8, 8
        );

        graphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.getX(), this.getY() - (float)this.scrollAmount());

        int logicMouseX = mouseX - getX();
        int logicMouseY = mouseY - getY() + (int)scrollAmount();

        for (int id = 1; id <= EmojiRegistry.INSTANCE.getSpriteCount(); id++) {
            int row = (id - 1) / columns;
            int col = (id - 1) % columns;

            int cellX = (col * (ITEM_SIZE + ITEM_SPACING_X)) + realPaddingX;
            int cellY = (row * (ITEM_SIZE + ITEM_SPACING_Y)) + PADDING_Y;

            if (selectedId == id) {
                graphics.fill(cellX - ITEM_PADDING, cellY - ITEM_PADDING,
                    cellX + ITEM_SIZE + ITEM_PADDING, cellY + ITEM_SIZE + ITEM_PADDING,
                    0xFF999999
                );
            } else if (cellX <= logicMouseX && cellX + ITEM_SIZE >= logicMouseX
                && cellY <= logicMouseY && cellY + ITEM_SIZE >= logicMouseY
                && 0 <= logicMouseY && getHeight() >= logicMouseY) {
                graphics.fill(cellX - ITEM_PADDING, cellY - ITEM_PADDING,
                    cellX + ITEM_SIZE + ITEM_PADDING, cellY + ITEM_SIZE + ITEM_PADDING,
                    0xFFDDDD99
                );
            }

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EmojiRegistry.INSTANCE.getSprite(id),
                cellX, cellY, ITEM_SIZE, ITEM_SIZE);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();
        this.extractScrollbar(graphics, mouseX, mouseY);
    }

    protected void repositionEntries() {
        columns = (width - PADDING_X * 2 - scrollbarWidth() - ITEM_SIZE) / (ITEM_SIZE + ITEM_SPACING_X) + 1;
        realPaddingX = (width - scrollbarWidth() - (ITEM_SIZE + (ITEM_SIZE + ITEM_SPACING_X) * (columns - 1))) / 2;
        contentHeight = ((int)Math.ceil(EmojiRegistry.INSTANCE.getSpriteCount() / (float)columns) - 1) * (ITEM_SIZE + ITEM_SPACING_Y)
            + ITEM_SIZE + PADDING_Y * 2;
    }

    public void updateSizeAndPosition(final int width, final int height, final int x, final int y) {
        this.setSize(width, height);
        this.setPosition(x, y);
        this.repositionEntries();
//        if (this.getSelected() != null) {
//            this.scrollToEntry(this.getSelected());
//        }

        this.refreshScrollAmount();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int logicMouseX = (int)event.x() - getX();
        int logicMouseY = (int)event.y() - getY() + (int)scrollAmount();
        for (int id = 1; id <= EmojiRegistry.INSTANCE.getSpriteCount(); id++) {
            int row = (id - 1) / columns;
            int col = (id - 1) % columns;
            int cellX = (col * (ITEM_SIZE + ITEM_SPACING_X)) + realPaddingX;
            int cellY = (row * (ITEM_SIZE + ITEM_SPACING_Y)) + PADDING_Y;
            if (cellX <= logicMouseX && cellX + ITEM_SIZE >= logicMouseX
                && cellY <= logicMouseY && cellY + ITEM_SIZE >= logicMouseY
                && 0 <= logicMouseY && getHeight() >= logicMouseY) {
                if (selectedId != id) {
                    selectedId = id;
                    onSelectionChange.accept(id);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    public void setSelectedId(int id) {
        this.selectedId = id;
    }

    public int getSelectedId() {
        return this.selectedId;
    }

    @Override
    protected int contentHeight() {
        return contentHeight;
    }

    @Override
    protected boolean scrollable() {
        return true;
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}
