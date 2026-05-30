package cn.zbx1425.worldcomment.util;

import cn.zbx1425.worldcomment.mixin.KeyMappingAccessor;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyMappingUtil {

    public static boolean isKeyDown(KeyMapping keyMapping) {
        InputConstants.Key key = ((KeyMappingAccessor)keyMapping).getKey();
        return key.getType() == InputConstants.Type.KEYSYM
            ? InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key.getValue())
            : keyMapping.isDown();
    }
}
