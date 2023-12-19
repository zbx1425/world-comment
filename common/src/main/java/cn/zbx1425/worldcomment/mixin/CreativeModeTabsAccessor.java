package cn.zbx1425.worldcomment.mixin;

import org.spongepowered.asm.mixin.Mixin;

#if MC_VERSION >= "12000"
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {

    @Accessor
    static ResourceKey<CreativeModeTab> getTOOLS_AND_UTILITIES() {
        throw new AssertionError();
    }
}

#else

import cn.zbx1425.worldcomment.Main;
@Mixin(Main.class)
public interface CreativeModeTabsAccessor {

}
#endif