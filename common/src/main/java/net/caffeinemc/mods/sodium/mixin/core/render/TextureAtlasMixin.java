package net.caffeinemc.mods.sodium.mixin.core.render;

import net.caffeinemc.mods.sodium.client.render.texture.ExtendedTextureAtlas;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinder;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinderImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements ExtendedTextureAtlas {
    @Shadow
    @Final
    private ResourceLocation location;

    @Shadow
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Shadow
    @Nullable
    private TextureAtlasSprite missingSprite;

    @Inject(method = "upload", at = @At("RETURN"))
    private void sodium$deleteSpriteFinder(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        if (this.location.equals(TextureAtlas.LOCATION_BLOCKS)) {
            SpriteFinderCache.resetSpriteFinder();
        }
    }

    @Override
    public SodiumSpriteFinder sodium$getSpriteFinder() {
        return new SodiumSpriteFinderImpl(this.texturesByName, this.missingSprite);
    }
}
