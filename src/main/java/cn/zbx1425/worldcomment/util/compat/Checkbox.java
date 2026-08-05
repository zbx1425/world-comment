package cn.zbx1425.worldcomment.util.compat;

#if MC_VERSION < "12003"

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class Checkbox {

    public static Checkbox.Builder builder(Component var0, Font var1) {
        return new Checkbox.Builder(var0, var1);
    }

    public static class Builder {
        private final Component message;
        private int x;
        private int y;
        private boolean selected = false;

        public Builder(Component var1, Font ignored) {
            this.message = var1;
        }

        public Checkbox.Builder pos(int var1, int var2) {
            this.x = var1;
            this.y = var2;
            return this;
        }

        public Checkbox.Builder selected(boolean var1) {
            this.selected = var1;
            return this;
        }

        public net.minecraft.client.gui.components.Checkbox build() {
            net.minecraft.client.gui.components.Checkbox var1 =
                    new net.minecraft.client.gui.components.Checkbox(this.x, this.y, 800, 20,
                            this.message, this.selected, true);
            return var1;
        }
    }
}

#endif
