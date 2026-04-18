package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {

    // NeoForge multipart models have a render-type-aware getQuads(...) path, but FRAPI's multipart
    // emitBlockQuads(...) recurses into child models directly and loses that top-level pass filtering.
    // Redirect only multipart models back through bufferDefaultModel(...) to query the
    // multipart model itself for the current pass. This is intentionally NeoForge-only.
    @Redirect(
            method = "renderModel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel;emitBlockQuads(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Lnet/fabricmc/fabric/api/renderer/v1/render/RenderContext;)V"
            )
    )
    private void sodium$renderMultipartThroughVanillaPath(FabricBakedModel model, BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        if (model instanceof MultiPartBakedModel) {
            ((AbstractBlockRenderContext) context).bufferDefaultModel((BakedModel) model, state);
            return;
        }

        model.emitBlockQuads(level, state, pos, randomSupplier, context);
    }
}
