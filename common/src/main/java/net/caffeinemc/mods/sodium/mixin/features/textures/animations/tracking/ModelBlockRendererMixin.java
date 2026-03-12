package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    /**
     * @reason Ensure sprites rendered through renderSmooth/renderFlat in immediate-mode are marked as active.
     * This doesn't affect vanilla to my knowledge, but mods can trigger it.
     * @author embeddedt
     */
    @Inject(method = "putQuadWithTint", at = @At("HEAD"))
    private void preRenderQuad(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad, CallbackInfo ci) {
        if (quad.materialInfo().sprite() != null) {
            SpriteUtil.INSTANCE.markSpriteActive(quad.materialInfo().sprite());
        }
    }
}
