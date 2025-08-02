package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension;
import net.caffeinemc.mods.sodium.client.render.texture.TextureAtlasSpriteTickerExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin implements SpriteContentsExtension {
    @Shadow
    @Final
    @Nullable
    private SpriteContents.AnimatedTexture animatedTexture;

    @Unique
    private boolean active;
    @Unique
    private TextureAtlasSpriteTickerExtension ticker;

    @Override
    public void sodium$setActive(boolean value) {
        this.active = value;
        if (this.ticker != null && value) {
            this.ticker.sodium$ensureUpload();
        }
    }

    @Override
    public boolean sodium$hasAnimation() {
        return this.animatedTexture != null;
    }

    @Override
    public boolean sodium$isActive() {
        return this.active;
    }

    @Override
    public void sodium$setTicker(TextureAtlasSpriteTickerExtension ticker) {
        this.ticker = ticker;
    }
}
