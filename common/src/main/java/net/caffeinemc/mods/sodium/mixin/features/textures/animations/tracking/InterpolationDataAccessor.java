package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.InterpolationData.class)
public interface InterpolationDataAccessor {
    @Invoker("uploadInterpolatedFrame")
    void sodium$uploadInterpolatedFrame(int x, int y, SpriteContents.Ticker ticker);
}
