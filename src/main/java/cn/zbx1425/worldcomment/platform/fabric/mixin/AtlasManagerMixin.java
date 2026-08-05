package cn.zbx1425.worldcomment.fabric.mixin;

import cn.zbx1425.worldcomment.data.client.EmojiRegistry;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(AtlasManager.class)
public class AtlasManagerMixin {

    @Shadow
    @Final
    private static List<AtlasManager.AtlasConfig> KNOWN_ATLASES;

    static {
        List<AtlasManager.AtlasConfig> configs = new ArrayList<>(KNOWN_ATLASES);
        configs.add(new AtlasManager.AtlasConfig(EmojiRegistry.ATLAS_TEXTURE_ID, EmojiRegistry.ATLAS_ID, true));
        KNOWN_ATLASES = List.copyOf(configs);
    }
}
