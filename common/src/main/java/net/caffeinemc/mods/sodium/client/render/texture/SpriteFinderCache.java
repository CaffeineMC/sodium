package net.caffeinemc.mods.sodium.client.render.texture;

import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;

/**
 * Caches {@link SpriteFinder}s for maximum efficiency. They must be refreshed after each resource reload.
 *
 * <p><b>This class should not be used during a resource reload</b>, as returned SpriteFinders may be null or outdated.
 */
public class SpriteFinderCache {
    private static SpriteFinder blockAtlasSpriteFinder;

    public static SpriteFinder forBlockAtlas() {
        if (blockAtlasSpriteFinder == null) {
            blockAtlasSpriteFinder = SpriteFinder.get(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
        }

        return blockAtlasSpriteFinder;
    }

    public static void resetSpriteFinder() {
        blockAtlasSpriteFinder = null;
    }
}
