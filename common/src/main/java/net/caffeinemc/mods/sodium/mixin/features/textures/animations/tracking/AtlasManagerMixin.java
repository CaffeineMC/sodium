package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    // This is used to catch the fire sprite when an entity is on fire and there's no fire blocks in the scene.
    @Inject(method = "get", at = @At(value = "RETURN"))
    private void sodium$catchUsedSprites(Material material, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (cir.getReturnValue() != null) {
            SpriteUtil.INSTANCE.markSpriteActive(cir.getReturnValue());
        }
    }
}
