package cn.zbx1425.worldcomment.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;
import java.util.function.Supplier;

public class GroupedItem extends Item {

    public final Supplier< #if MC_VERSION >= "12000" RegistryKey<CreativeModeTab> #else CreativeModeTab #endif > tabSupplier;

    public GroupedItem(
            Supplier< #if MC_VERSION >= "12000" RegistryKey<CreativeModeTab> #else CreativeModeTab #endif > tabSupplier,
            Function<Properties, Properties> properties) {
        super(
#if MC_VERSION >= "12000"
                properties.apply(new Properties())
#else
                properties.apply(new Properties().tab(tabSupplier.get()))
#endif
        );
        this.tabSupplier = tabSupplier;
    }
}
