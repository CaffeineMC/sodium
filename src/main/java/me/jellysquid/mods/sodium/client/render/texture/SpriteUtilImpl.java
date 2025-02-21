package me.jellysquid.mods.sodium.client.render.texture;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SpriteUtilImpl implements SpriteUtil {
    @Override
    public void markSpriteActive(@NotNull Sprite sprite) {
        Objects.requireNonNull(sprite);

        ((SpriteContentsExtended) sprite.getContents()).sodium$setActive(true);
    }

    @Override
    public boolean hasAnimation(@NotNull Sprite sprite) {
        Objects.requireNonNull(sprite);

        return ((SpriteContentsExtended) sprite.getContents()).sodium$hasAnimation();
    }
}