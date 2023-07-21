package cn.zbx1425.worldcomment.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class CommentTypeButton extends Button {

    public int commentType;

    public static int BTN_WIDTH = 40;
    public static int BTN_HEIGHT = 40;

    protected CommentTypeButton(int x, int y, int type, OnPress onPress) {
        super(x, y, BTN_WIDTH, 20,
                Component.translatable("gui.worldcomment.comment_type." + type),
                onPress, Supplier::get);
        this.commentType = type;
    }
}
