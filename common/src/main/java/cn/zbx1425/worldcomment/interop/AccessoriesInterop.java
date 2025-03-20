package cn.zbx1425.worldcomment.interop;

import cn.zbx1425.worldcomment.Main;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.function.BooleanSupplier;

public class AccessoriesInterop {

    private static BooleanSupplier isWearingEyeglass;

    static {
        isWearingEyeglass = () -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;
            return player.getInventory().armor.get(3).is(Main.ITEM_COMMENT_EYEGLASS.get());
        };
        try {
            Class<?> ignored = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            isWearingEyeglass = () -> {
                Player player = Minecraft.getInstance().player;
                if (player == null) return false;
                if (player.getInventory().armor.get(3).is(Main.ITEM_COMMENT_EYEGLASS.get())) return true;
                AccessoriesCapability cap = AccessoriesCapability.get(player);
                if (cap == null) return false;
                return !cap.getEquipped(Main.ITEM_COMMENT_EYEGLASS.get()).isEmpty();
            };
        } catch (Exception ignored) {

        }
    }

    public static boolean isWearingEyeglass() {
        return isWearingEyeglass.getAsBoolean();
    }
}
