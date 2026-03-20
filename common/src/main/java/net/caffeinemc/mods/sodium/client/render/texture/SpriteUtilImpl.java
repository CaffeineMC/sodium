package net.caffeinemc.mods.sodium.client.render.texture;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class SpriteUtilImpl implements SpriteUtil {
    @Override
    public void markSpriteActive(@NonNull TextureAtlasSprite sprite) {
        Objects.requireNonNull(sprite);

        ((SpriteContentsExtension) sprite.contents()).sodium$setActive(true);
    }

    @Override
    public boolean hasAnimation(@NonNull TextureAtlasSprite sprite) {
        Objects.requireNonNull(sprite);

        return ((SpriteContentsExtension) sprite.contents()).sodium$hasAnimation();
    }
}