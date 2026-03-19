package net.caffeinemc.mods.sodium.mixin.core.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextureAtlas.class)
public interface TextureAtlasAccessor {
    @Invoker("getWidth")
    int sodium$getWidth();

    @Invoker("getHeight")
    int sodium$getHeight();
}
