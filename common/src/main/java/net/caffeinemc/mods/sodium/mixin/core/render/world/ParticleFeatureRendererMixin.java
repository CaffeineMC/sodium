package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleFeatureRenderer.class)
public abstract class ParticleFeatureRendererMixin {
    /**
     * Vanilla's {@code prepareRenderPass} only sets {@code Projection}, {@code Fog} and {@code Sampler2}, and relies on the binding for {@code Globals} and {@code Lighting} being inherited from some other calls before. This is often (but not always) the vanilla chunk render, whose {@code SOLID_TERRAIN} program does the same binding for {@code Globals} as the particle program expects. Sodium replaces the chunk render path that would be doing {@link RenderSystem#bindDefaultUniforms}, so that binding is left in whatever state it is before particle rendering, which is not the different state that particle rendering expects it to be after vanilla terrain rendering. Shaders that import {@code globals.glsl} into their particle shaders then read garbage (presumably zero) for {@code GameTime} and other {@code Globals} and {@code Lighting} fields. See <a href="https://github.com/CaffeineMC/sodium/issues/3612">this issue.</a>
     */
    @Inject(method = "prepareRenderPass", at = @At("HEAD"))
    private void sodium$bindDefaultUniforms(RenderPass renderPass, CallbackInfo ci) {
        GpuBuffer globalUniform = RenderSystem.getGlobalSettingsUniform();
        if (globalUniform != null) {
            renderPass.setUniform("Globals", globalUniform);
        }

        GpuBufferSlice shaderLights = RenderSystem.getShaderLights();
        if (shaderLights != null) {
            renderPass.setUniform("Lighting", shaderLights);
        }
    }
}
