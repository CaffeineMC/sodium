package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlRenderPass")
public interface GlRenderPassAccessor {
    @Accessor
    GlRenderPipeline getPipeline();
}
