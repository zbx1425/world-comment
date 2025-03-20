package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
#if MC_VERSION >= "12000" import net.minecraft.core.registries.Registries; #endif
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceKey;

public class CommentEyeglassItem extends Item implements GroupedItem {

    public CommentEyeglassItem() {
        super(
            GroupedItem.createProperties(properties -> injectFabricSettings(
                    properties.stacksTo(1)),
                    CommentEyeglassItem::getTabImpl)
        );
    }

    public static Properties injectFabricSettings(Properties properties) {
        return properties;
    }

    @Override
    public #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTab() {
        return getTabImpl();
    }

    public static #if MC_VERSION >= "12000" ResourceKey<CreativeModeTab> #else CreativeModeTab #endif getTabImpl() {
        #if MC_VERSION >= "12000"
            return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Main.vanillaId("tools_and_utilities"));
        #else
            return CreativeModeTab.TAB_MISC;
        #endif
    }
}
