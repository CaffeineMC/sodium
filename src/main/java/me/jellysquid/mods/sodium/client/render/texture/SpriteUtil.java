package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.Nullable;

// Kept for mod compatibility, to be removed in next major release.
@Deprecated(forRemoval = true)
public class SpriteUtil {
    @Deprecated(forRemoval = true)
    public static void markSpriteActive(@Nullable Sprite sprite) {
        if (sprite != null) {
            net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }

    @Deprecated(forRemoval = true)
    public static boolean hasAnimation(@Nullable Sprite sprite) {
        if (sprite != null) {
            return net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.hasAnimation(sprite);
        }

        return false;
    }
}
