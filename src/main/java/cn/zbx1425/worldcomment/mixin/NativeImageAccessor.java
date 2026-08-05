package cn.zbx1425.worldcomment.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {

#if MC_VERSION >= 12106
    @Invoker
    boolean invokeWriteToChannel(WritableByteChannel channel) throws IOException;
#endif
}
