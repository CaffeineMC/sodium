package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    /**
     * @reason Ensure sprites rendered through renderSmooth/renderFlat in immediate-mode are marked as active.
     * This doesn't affect vanilla to my knowledge, but mods can trigger it.
     * @author embeddedt
     */
    @Inject(method = "putQuadData", at = @At("HEAD"))
    private void preRenderQuad(BlockAndTintGetter blockAndTintGetter, BlockState blockState, BlockPos blockPos, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad, float[] fs, int[] is, int i, CallbackInfo ci) {
        if (quad.sprite() != null) {
            SpriteUtil.INSTANCE.markSpriteActive(quad.sprite());
        }
    }
}
