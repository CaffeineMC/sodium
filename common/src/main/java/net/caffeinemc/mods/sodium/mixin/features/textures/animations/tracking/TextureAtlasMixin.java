package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {
    @ModifyReturnValue(method = "getSprite", at = @At("RETURN"))
    private TextureAtlasSprite preReturnSprite(TextureAtlasSprite sprite) {
        if (sprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }

        return sprite;
    }
}
