package net.caffeinemc.mods.sodium.client.render.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Caches {@link SpriteFinder}s for maximum efficiency. They must be refreshed after each resource reload.
 *
 * <p><b>This class should not be used during a resource reload</b>, as returned SpriteFinders may be null or outdated.
 */
public class SpriteFinderCache {
    private static SodiumSpriteFinder blockAtlasSpriteFinder;

    public static SodiumSpriteFinder forBlockAtlas() {
        if (blockAtlasSpriteFinder == null) {
            blockAtlasSpriteFinder = ((ExtendedTextureAtlas) Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)).sodium$getSpriteFinder();
        }

        return blockAtlasSpriteFinder;
    }

    public static void resetSpriteFinder() {
        blockAtlasSpriteFinder = null;
    }
}
