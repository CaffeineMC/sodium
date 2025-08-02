package net.caffeinemc.mods.sodium.client.render.texture;

import net.minecraft.resources.ResourceLocation;

public interface SpriteContentsTickerExtension {
    void sodium$ensureUpload(ResourceLocation atlas, int x, int y);
}
