package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public interface GlCommandEncoderAccessor {
    @Invoker("applyPipelineState")
    void sodium$applyPipelineState(RenderPipeline pipeline);

    @Accessor("lastProgram")
    void sodium$setLastProgram(GlProgram program);
}
