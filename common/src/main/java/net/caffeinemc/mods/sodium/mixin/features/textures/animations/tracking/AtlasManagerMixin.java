package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    // This is used to catch the fire sprite when an entity is on fire and there's no fire blocks in the scene.
    @ModifyReturnValue(method = "get", at = @At(value = "RETURN"))
    private TextureAtlasSprite markSpriteAsActive(TextureAtlasSprite sprite) {
        if (sprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }

        return sprite;
    }
}
