package cn.zbx1425.worldcomment.util.compat;

#if MC_VERSION < "12000"

import net.minecraft.network.chat.Component;

public class Button {

    public static Button.Builder builder(Component var0, net.minecraft.client.gui.components.Button.OnPress var1) {
        return new Button.Builder(var0, var1);
    }

    public static class Builder {
        private final Component message;
        private final net.minecraft.client.gui.components.Button.OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        public Builder(Component var1,  net.minecraft.client.gui.components.Button.OnPress var2) {
            this.message = var1;
            this.onPress = var2;
        }

        public Button.Builder pos(int var1, int var2) {
            this.x = var1;
            this.y = var2;
            return this;
        }

        public Button.Builder width(int var1) {
            this.width = var1;
            return this;
        }

        public Button.Builder size(int var1, int var2) {
            this.width = var1;
            this.height = var2;
            return this;
        }

        public Button.Builder bounds(int var1, int var2, int var3, int var4) {
            return this.pos(var1, var2).size(var3, var4);
        }

        public net.minecraft.client.gui.components.Button build() {
            net.minecraft.client.gui.components.Button var1 = new net.minecraft.client.gui.components.Button(this.x, this.y, this.width, this.height, this.message, this.onPress);
            return var1;
        }
    }
}

#endif
