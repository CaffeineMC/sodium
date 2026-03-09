package net.caffeinemc.mods.sodium.mixin.core.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {
    @Accessor("width")
    int sodium$getWidth();

    @Accessor("height")
    int sodium$getHeight();
}
