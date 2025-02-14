package net.caffeinemc.mods.sodium.api.texture;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Utility functions for querying sprite information and updating per-frame information about sprite visibility.
 */
@ApiStatus.Experimental
public interface SpriteUtil {
    SpriteUtil INSTANCE = DependencyInjection.load(SpriteUtil.class,
            "net.caffeinemc.mods.sodium.client.render.texture.SpriteUtilImpl");
    
    /**
     * Marks the sprite as "active", meaning that it is visible during this frame and should have the animation
     * state updated. Mods which perform their own rendering without the use of Minecraft's helpers will need to
     * call this method once every frame, when their sprite is actively being used in rendering.
     * @param sprite The sprite to mark as active
     */
    void markSpriteActive(@NotNull TextureAtlasSprite sprite);

    /**
     * Returns if the provided sprite has an animation.
     * 
     * @param sprite The sprite to query an animation for
     * @return {@code true} if the provided sprite has an animation, otherwise {@code false}
     */
    boolean hasAnimation(@NotNull TextureAtlasSprite sprite);
}