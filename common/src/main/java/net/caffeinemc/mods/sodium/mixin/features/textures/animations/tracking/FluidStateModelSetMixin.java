package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidStateModelSet.class)
public class FluidStateModelSetMixin {
    // Catches fluid sprites accessed outside the chunk fluid rendering path, e.g. FramedBlocks' framed tank.
    @ModifyReturnValue(method = "get", at = @At(value = "RETURN"))
    private FluidModel markSpritesAsActive(FluidModel model) {
        if (model == null) {
            return model;
        }

        SpriteUtil.INSTANCE.markSpriteActive(model.stillMaterial().sprite());
        SpriteUtil.INSTANCE.markSpriteActive(model.flowingMaterial().sprite());

        Material.Baked overlay = model.overlayMaterial();
        if (overlay != null) {
            SpriteUtil.INSTANCE.markSpriteActive(overlay.sprite());
        }
        return model;
    }
}
