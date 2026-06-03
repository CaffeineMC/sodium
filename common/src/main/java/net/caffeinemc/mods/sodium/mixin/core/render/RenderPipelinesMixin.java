package net.caffeinemc.mods.sodium.mixin.core.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderPipelines.class)
public class RenderPipelinesMixin {
    /**
     * This fixes a change seemingly randomly done in 26.2 Pre-Release 3, that removes the scale factor from CRUMBLING. Vanilla terrain seemingly doesn't need it, but we still do.
     * This should be monitored closely in case it changes...
     */
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;withDepthStencilState(Lcom/mojang/blaze3d/pipeline/DepthStencilState;)Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;"))
    private static RenderPipeline.Builder sodium$undoCrumbling(RenderPipeline.Builder instance, DepthStencilState depthStencilState, Operation<RenderPipeline.Builder> original) {
        // This is a very bad way to check this. However, Builder does not have return values to read location.
        if (depthStencilState.depthBiasConstant() == 10.0f && depthStencilState.depthBiasScaleFactor() == 0.0f) {
            depthStencilState = new DepthStencilState(depthStencilState.depthTest(), depthStencilState.writeDepth(), 1.0f, depthStencilState.depthBiasConstant());
        }
        return original.call(instance, depthStencilState);
    }
}
