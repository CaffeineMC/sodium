package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidStateModelSet.class)
public class FluidStateModelSetMixin {
    // Catches fluid sprites accessed outside the chunk fluid rendering path, e.g. FramedBlocks' framed tank.
    @Inject(method = "get", at = @At(value = "RETURN"))
    private void sodium$catchUsedSprites(FluidState state, CallbackInfoReturnable<FluidModel> cir) {
        FluidModel model = cir.getReturnValue();
        if (model == null) {
            return;
        }

        SpriteUtil.INSTANCE.markSpriteActive(model.stillMaterial().sprite());
        SpriteUtil.INSTANCE.markSpriteActive(model.flowingMaterial().sprite());

        Material.Baked overlay = model.overlayMaterial();
        if (overlay != null) {
            SpriteUtil.INSTANCE.markSpriteActive(overlay.sprite());
        }
    }
}
