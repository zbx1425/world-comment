package cn.zbx1425.worldcomment.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {

#if MC_VERSION >= "12106"
    @Invoker
    void invokeSubmitBlit(RenderPipeline pipeline, GpuTextureView atlasTexture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color);
#endif
}
